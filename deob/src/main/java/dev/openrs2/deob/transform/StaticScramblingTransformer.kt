package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.getExpression
import dev.openrs2.asm.sequential
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.remap.TypedRemapper
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.math.max

class StaticScramblingTransformer : Transformer() {
    private data class Field(val node: FieldNode, val initializer: InsnList, val version: Int, val maxStack: Int) {
        val dependencies = initializer.asSequence()
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.GETSTATIC }
            .map(::MemberRef)
            .toSet()
    }

    private val fields = mutableMapOf<MemberRef, Field>()
    private val fieldClasses = mutableMapOf<MemberRef, String>()
    private val methodClasses = mutableMapOf<MemberRef, String>()
    private var nextStaticClass: ClassNode? = null
    private var nextClinit: MethodNode? = null
    private val staticClasses = mutableListOf<ClassNode>()

    private fun nextClass(): Pair<ClassNode, MethodNode> {
        var clazz = nextStaticClass
        if (clazz != null && (clazz.fields.size + clazz.methods.size) < MAX_FIELDS_AND_METHODS) {
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
        clazz.access = Opcodes.ACC_PUBLIC or Opcodes.ACC_SUPER
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

    private fun MethodNode.extractEntryExitBlocks(): List<AbstractInsnNode> {
        /*
         * Most (or all?) of the <clinit> methods have "simple" initializers
         * that we're capable of moving in the first and last basic blocks of
         * the method. The last basic block is always at the end of the code
         * and ends in a RETURN. This allows us to avoid worrying about making
         * a full basic block control flow graph here.
         */
        val entry = instructions.takeWhile { it.sequential }

        val last = instructions.lastOrNull()
        if (last == null || last.opcode != Opcodes.RETURN) {
            return entry
        }

        val exit = instructions.toList()
            .dropLast(1)
            .takeLastWhile { it.sequential }

        return entry.plus(exit)
    }

    private fun MethodNode.extractInitializers(owner: String): Pair<Map<MemberDesc, InsnList>, Set<MemberDesc>> {
        val entryExitBlocks = extractEntryExitBlocks()

        val simpleInitializers = mutableMapOf<MemberDesc, InsnList>()
        val complexInitializers = instructions.asSequence()
            .filter { !entryExitBlocks.contains(it) }
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.GETSTATIC && it.owner == owner && it.name !in TypedRemapper.EXCLUDED_FIELDS }
            .map(::MemberDesc)
            .toSet()

        val putstatics = entryExitBlocks
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.PUTSTATIC && it.owner == owner && it.name !in TypedRemapper.EXCLUDED_FIELDS }

        for (putstatic in putstatics) {
            val desc = MemberDesc(putstatic)
            if (simpleInitializers.containsKey(desc) || complexInitializers.contains(desc)) {
                continue
            }

            // TODO(gpe): use a filter here (pure with no *LOADs?)
            val expr = getExpression(putstatic) ?: continue

            val initializer = InsnList()
            for (insn in expr) {
                instructions.remove(insn)
                initializer.add(insn)
            }
            instructions.remove(putstatic)
            initializer.add(putstatic)

            simpleInitializers[desc] = initializer
        }

        return Pair(simpleInitializers, complexInitializers)
    }

    private fun spliceInitializers() {
        val done = mutableSetOf<MemberRef>()
        for ((ref, field) in fields) {
            spliceInitializers(done, ref, field)
        }
    }

    private fun spliceInitializers(done: MutableSet<MemberRef>, ref: MemberRef, field: Field) {
        if (!done.add(ref)) {
            return
        }

        for (dependency in field.dependencies) {
            val dependencyField = fields[dependency] ?: continue
            spliceInitializers(done, dependency, dependencyField)
        }

        val (clazz, clinit) = nextClass()
        clazz.fields.add(field.node)
        clazz.version = ClassVersionUtils.maxVersion(clazz.version, field.version)
        clinit.instructions.insertBefore(clinit.instructions.last, field.initializer)
        clinit.maxStack = max(clinit.maxStack, field.maxStack)

        fieldClasses[ref] = clazz.name
    }

    override fun preTransform(classPath: ClassPath) {
        fields.clear()
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
                if (clazz.name in TypedRemapper.EXCLUDED_CLASSES) {
                    continue
                }

                val clinit = clazz.methods.find { it.name == "<clinit>" }
                val (simpleInitializers, complexInitializers) = clinit?.extractInitializers(clazz.name)
                    ?: Pair(emptyMap(), emptySet())

                clazz.fields.removeIf { field ->
                    if (field.access and Opcodes.ACC_STATIC == 0) {
                        return@removeIf false
                    } else if (field.name in TypedRemapper.EXCLUDED_METHODS) {
                        return@removeIf false
                    }

                    val desc = MemberDesc(field)
                    if (complexInitializers.contains(desc)) {
                        return@removeIf false
                    }

                    val initializer = simpleInitializers[desc] ?: InsnList()
                    val maxStack = clinit?.maxStack ?: 0

                    val ref = MemberRef(clazz, field)
                    fields[ref] = Field(field, initializer, clazz.version, maxStack)

                    return@removeIf true
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
                    staticClass.version = ClassVersionUtils.maxVersion(staticClass.version, clazz.version)

                    methodClasses[MemberRef(clazz, method)] = staticClass.name
                    return@removeIf true
                }
            }

            spliceInitializers()

            for (clazz in staticClasses) {
                library.add(clazz)
            }
        }
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            when (insn) {
                is FieldInsnNode -> insn.owner = fieldClasses.getOrDefault(MemberRef(insn), insn.owner)
                is MethodInsnNode -> insn.owner = methodClasses.getOrDefault(MemberRef(insn), insn.owner)
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Moved ${fieldClasses.size} fields and ${methodClasses.size} methods" }
    }

    companion object {
        private val logger = InlineLogger()
        private const val MAX_FIELDS_AND_METHODS = 500
    }
}
