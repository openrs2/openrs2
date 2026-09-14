package org.openrs2.asm.classpath

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.ClassVersionUtils
import org.openrs2.asm.MemberRef
import org.openrs2.asm.nextReal
import org.openrs2.asm.remap
import java.util.SortedMap
import java.util.TreeMap
import kotlin.math.max

public class LibraryRemapper(
    private val remapper: ExtendedRemapper,
    private var classes: SortedMap<String, ClassNode>
) {
    private class Initializer(val instructions: InsnList, val maxStack: Int) {
        val dependencies = instructions.asSequence()
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.GETSTATIC }
            .map(::MemberRef)
            .toSet()
    }

    private class Field(val owner: String, val node: FieldNode, val version: Int, val initializer: Initializer?)
    private class Method(val owner: String, val node: MethodNode, val version: Int)

    private val fields = mutableMapOf<MemberRef, Field>()
    private val splicedFields = mutableSetOf<MemberRef>()
    private val methods = mutableListOf<Method>()

    public fun remap(): SortedMap<String, ClassNode> {
        // extract static fields/methods that are being moved between classes
        extractFields()
        extractMethods()

        // map remaining fields/methods
        for (clazz in classes.values) {
            clazz.remap(remapper)
        }

        classes = classes.mapKeysTo(TreeMap()) { (_, clazz) -> clazz.name }

        // splice static fields/methods into their new classes
        spliceFields()
        spliceMethods()

        // remove empty <clinit> methods (so EmptyClassTransformer works later)
        removeEmptyClinitMethods()

        return classes
    }

    private fun extractFields() {
        for (clazz in classes.values) {
            if (clazz.name.contains('/')) {
                continue
            }

            clazz.fields.removeIf { field ->
                // do nothing if the field is not moved between classes
                val oldOwner = remapper.mapType(clazz.name)
                val newOwner = remapper.mapFieldOwner(clazz.name, field.name, field.desc)
                if (oldOwner == newOwner) {
                    return@removeIf false
                }

                /*
                 * Remove the initializer (if present) from the old owner's
                 * <clinit> method.
                 */
                val initializer = extractInitializer(clazz, field)

                /*
                 * Map the field (it won't be caught by a ClassNode::remap call
                 * during the main pass).
                 */
                field.remap(remapper, clazz.name)

                /*
                 * Store the field so it can be spliced into its new class
                 * later. We key on the new owner/name/descriptor, rather than
                 * the old one, as spliceFields's dependency tracking runs
                 * after the Initializer::dependencies has been mapped.
                 */
                val newMember = MemberRef(newOwner, field.name, field.desc)
                fields[newMember] = Field(newOwner, field, clazz.version, initializer)

                // remove the field from its old class
                return@removeIf true
            }
        }
    }

    private fun extractInitializer(clazz: ClassNode, field: FieldNode): Initializer? {
        val clinit = clazz.methods.find { it.name == "<clinit>" } ?: return null
        val initializer = remapper.getFieldInitializer(clazz.name, field.name, field.desc) ?: return null

        val list = InsnList()
        for (insn in initializer) {
            /*
             * Remove initializer from <clinit>. It is stored alongside the
             * FieldNode so it can be spliced into its new class later.
             */
            clinit.instructions.remove(insn)
            list.add(insn)

            /*
             * Remap the initializer (it won't be caught by a ClassNode::remap
             * call during the main pass).
             */
            insn.remap(remapper)
        }

        return Initializer(list, clinit.maxStack)
    }

    private fun extractMethods() {
        for (clazz in classes.values) {
            if (clazz.name.contains('/')) {
                continue
            }

            clazz.methods.removeIf { method ->
                // do nothing if the method is not moved between classes
                val oldOwner = remapper.mapType(clazz.name)
                val newOwner = remapper.mapMethodOwner(clazz.name, method.name, method.desc)
                if (oldOwner == newOwner) {
                    return@removeIf false
                }

                /*
                 * Map the method (it won't be caught by a ClassNode::remap call
                 * during the main pass).
                 */
                method.remap(remapper, clazz.name)

                // store the method
                methods += Method(newOwner, method, clazz.version)

                // remove the method from its old class
                return@removeIf true
            }
        }
    }

    private fun spliceFields() {
        for (member in fields.keys) {
            spliceField(member)
        }
    }

    private fun spliceField(member: MemberRef) {
        if (!splicedFields.add(member)) {
            return
        }

        val field = fields[member] ?: return
        val clazz = classes.computeIfAbsent(field.owner, ::createClass)

        if (field.initializer != null) {
            for (dependency in field.initializer.dependencies) {
                spliceField(dependency)
            }

            val clinit = clazz.methods.find { it.name == "<clinit>" } ?: createClinitMethod(clazz)
            check(hasSingleTailExit(clinit.instructions)) {
                "${clazz.name}'s <clinit> method does not have a single RETURN at the end of the InsnList"
            }

            clinit.maxStack = max(clinit.maxStack, field.initializer.maxStack)
            clinit.instructions.insertBefore(clinit.instructions.last, field.initializer.instructions)
        }

        clazz.version = ClassVersionUtils.max(clazz.version, field.version)
        clazz.fields.add(field.node)
    }

    private fun hasSingleTailExit(instructions: InsnList): Boolean {
        val insn = instructions.singleOrNull { it.opcode == Opcodes.RETURN }
        return insn != null && insn.nextReal == null
    }

    private fun spliceMethods() {
        for (method in methods) {
            val clazz = classes.computeIfAbsent(method.owner, ::createClass)
            clazz.version = ClassVersionUtils.max(clazz.version, method.version)
            clazz.methods.add(method.node)
        }
    }

    private fun removeEmptyClinitMethods() {
        for (clazz in classes.values) {
            val clinit = clazz.methods.find { it.name == "<clinit>" } ?: continue

            val first = clinit.instructions.firstOrNull { it.opcode != -1 }
            if (first != null && first.opcode == Opcodes.RETURN) {
                clazz.methods.remove(clinit)
            }
        }
    }

    private fun createClass(name: String): ClassNode {
        val clazz = ClassNode()
        clazz.version = Opcodes.V1_1
        clazz.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER or Opcodes.ACC_FINAL
        clazz.name = name
        clazz.superName = "java/lang/Object"
        clazz.interfaces = mutableListOf()
        clazz.innerClasses = mutableListOf()
        clazz.fields = mutableListOf()
        clazz.methods = mutableListOf()
        return clazz
    }

    private fun createClinitMethod(clazz: ClassNode): MethodNode {
        val clinit = MethodNode()
        clinit.access = Opcodes.ACC_STATIC
        clinit.name = "<clinit>"
        clinit.desc = "()V"
        clinit.exceptions = mutableListOf()
        clinit.parameters = mutableListOf()
        clinit.instructions = InsnList()
        clinit.instructions.add(InsnNode(Opcodes.RETURN))
        clinit.tryCatchBlocks = mutableListOf()

        clazz.methods.add(clinit)

        return clinit
    }
}
