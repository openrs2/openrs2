package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.remap.TypedRemapper
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.math.max

class StaticScramblingTransformer : Transformer() {
    private class FieldSet(val owner: ClassNode, val fields: List<FieldNode>, val clinit: MethodNode?) {
        val dependencies = clinit?.instructions
            ?.filterIsInstance<FieldInsnNode>()
            ?.filter { it.opcode == Opcodes.GETSTATIC && it.owner != owner.name }
            ?.mapTo(mutableSetOf(), ::MemberRef) ?: emptySet<MemberRef>()
    }

    private lateinit var inheritedFieldSets: DisjointSet<MemberRef>
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private val fieldSets = mutableMapOf<MemberRef, FieldSet>()
    private val fieldClasses = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()
    private val methodClasses = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()
    private var nextStaticClass: ClassNode? = null
    private var nextClinit: MethodNode? = null
    private val staticClasses = mutableListOf<ClassNode>()

    private fun nextClass(): Pair<ClassNode, MethodNode> {
        var clazz = nextStaticClass
        if (clazz != null && clazz.fields.size < MAX_FIELDS && clazz.methods.size < MAX_METHODS) {
            return Pair(clazz, nextClinit!!)
        }

        val clinit = MethodNode()
        clinit.access = Opcodes.ACC_STATIC
        clinit.name = "<clinit>"
        clinit.desc = "()V"
        clinit.exceptions = mutableListOf()
        clinit.parameters = mutableListOf()
        clinit.instructions = InsnList()
        clinit.instructions.add(InsnNode(Opcodes.RETURN))
        clinit.tryCatchBlocks = mutableListOf()

        clazz = ClassNode()
        clazz.version = Opcodes.V1_1
        clazz.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER or Opcodes.ACC_FINAL
        clazz.name = "Static${staticClasses.size + 1}"
        clazz.superName = "java/lang/Object"
        clazz.interfaces = mutableListOf()
        clazz.innerClasses = mutableListOf()
        clazz.fields = mutableListOf()
        clazz.methods = mutableListOf(clinit)

        staticClasses += clazz
        nextStaticClass = clazz
        nextClinit = clinit

        return Pair(clazz, clinit)
    }

    private fun spliceFields() {
        val done = mutableSetOf<FieldSet>()
        for (fieldSet in fieldSets.values) {
            spliceFields(done, fieldSet)
        }
    }

    private fun spliceFields(done: MutableSet<FieldSet>, fieldSet: FieldSet) {
        if (!done.add(fieldSet)) {
            return
        }

        for (dependency in fieldSet.dependencies) {
            val dependencyFieldSet = fieldSets[dependency] ?: continue
            spliceFields(done, dependencyFieldSet)
        }

        val (staticClass, staticClinit) = nextClass()
        staticClass.fields.addAll(fieldSet.fields)
        staticClass.version = ClassVersionUtils.max(staticClass.version, fieldSet.owner.version)

        if (fieldSet.clinit != null) {
            // remove tail RETURN
            val insns = fieldSet.clinit.instructions
            val last = insns.lastOrNull()
            if (last != null && last.opcode == Opcodes.RETURN) {
                insns.remove(last)
            }

            // replace any remaining RETURNs with a GOTO to the end of the method
            val end = LabelNode()
            insns.add(end)

            for (insn in insns) {
                if (insn.opcode == Opcodes.RETURN) {
                    insns.set(insn, JumpInsnNode(Opcodes.GOTO, end))
                }
            }

            // append just before the end of the static <clinit> RETURN
            staticClinit.instructions.insertBefore(staticClinit.instructions.last, insns)

            staticClinit.tryCatchBlocks.addAll(fieldSet.clinit.tryCatchBlocks)
            staticClinit.maxStack = max(staticClinit.maxStack, fieldSet.clinit.maxStack)
            staticClinit.maxLocals = max(staticClinit.maxLocals, fieldSet.clinit.maxLocals)
        }

        for (field in fieldSet.fields) {
            val partition = inheritedFieldSets[MemberRef(fieldSet.owner, field)]!!
            fieldClasses[partition] = staticClass.name
        }
    }

    override fun preTransform(classPath: ClassPath) {
        inheritedFieldSets = classPath.createInheritedFieldSets()
        inheritedMethodSets = classPath.createInheritedMethodSets()
        fieldSets.clear()
        fieldClasses.clear()
        methodClasses.clear()
        nextStaticClass = null
        staticClasses.clear()

        for (library in classPath.libraries) {
            // TODO(gpe): improve detection of the client library
            if ("client" !in library) {
                continue
            }

            for (clazz in library) {
                // TODO(gpe): exclude the JSObject class
                if (clazz.name == "jagex3/jagmisc/jagmisc") {
                    continue
                }

                val fields = clazz.fields.filter { it.access and Opcodes.ACC_STATIC != 0 }
                clazz.fields.removeAll(fields)

                val clinit = clazz.methods.find { it.name == "<clinit>" }
                if (clinit != null) {
                    clazz.methods.remove(clinit)
                }

                val fieldSet = FieldSet(clazz, fields, clinit)
                for (field in fields) {
                    val ref = MemberRef(clazz, field)
                    fieldSets[ref] = fieldSet
                }

                clazz.methods.removeIf { method ->
                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        return@removeIf false
                    } else if (method.access and Opcodes.ACC_NATIVE != 0) {
                        return@removeIf false
                    } else if (method.name in TypedRemapper.EXCLUDED_METHODS) {
                        return@removeIf false
                    }

                    val (staticClass, _) = nextClass()
                    staticClass.methods.add(method)
                    staticClass.version = ClassVersionUtils.max(staticClass.version, clazz.version)

                    val partition = inheritedMethodSets[MemberRef(clazz, method)]!!
                    methodClasses[partition] = staticClass.name
                    return@removeIf true
                }
            }

            spliceFields()

            for (clazz in staticClasses) {
                library.add(clazz)
            }
        }
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            when (insn) {
                is FieldInsnNode -> {
                    val partition = inheritedFieldSets[MemberRef(insn)]
                    if (partition != null) {
                        insn.owner = fieldClasses.getOrDefault(partition, insn.owner)
                    }
                }
                is MethodInsnNode -> {
                    val partition = inheritedMethodSets[MemberRef(insn)]
                    if (partition != null) {
                        insn.owner = methodClasses.getOrDefault(partition, insn.owner)
                    }
                }
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Moved ${fieldClasses.size} fields and ${methodClasses.size} methods" }
    }

    companion object {
        private val logger = InlineLogger()
        private const val MAX_FIELDS = 500
        private const val MAX_METHODS = 50
    }
}
