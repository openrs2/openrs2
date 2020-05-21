package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.ClassVersionUtils
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.getExpression
import dev.openrs2.asm.isSequential
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.Profile
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class StaticScramblingTransformer @Inject constructor(private val profile: Profile) : Transformer() {
    private data class Field(val node: FieldNode, val initializer: InsnList, val version: Int, val maxStack: Int) {
        val dependencies = initializer.asSequence()
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.GETSTATIC }
            .map(::MemberRef)
            .toSet()
    }

    private lateinit var inheritedFieldSets: DisjointSet<MemberRef>
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private val fields = mutableMapOf<DisjointSet.Partition<MemberRef>, Field>()
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

    private fun MethodNode.extractEntryExitBlocks(): List<AbstractInsnNode> {
        /*
         * Most (or all?) of the <clinit> methods have "simple" initializers
         * that we're capable of moving in the first and last basic blocks of
         * the method. The last basic block is always at the end of the code
         * and ends in a RETURN. This allows us to avoid worrying about making
         * a full basic block control flow graph here.
         */
        val entry = instructions.takeWhile { it.isSequential }

        val last = instructions.lastOrNull()
        if (last == null || last.opcode != Opcodes.RETURN) {
            return entry
        }

        val exit = instructions.toList()
            .dropLast(1)
            .takeLastWhile { it.isSequential }

        return entry.plus(exit)
    }

    private fun MethodNode.extractInitializers(owner: String): Pair<Map<MemberDesc, InsnList>, Set<MemberDesc>> {
        val entryExitBlocks = extractEntryExitBlocks()

        val simpleInitializers = mutableMapOf<MemberDesc, InsnList>()
        val complexInitializers = instructions.asSequence()
            .filter { !entryExitBlocks.contains(it) }
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.GETSTATIC && it.owner == owner }
            .filter { !profile.excludedFields.matches(it.owner, it.name, it.desc) }
            .map(::MemberDesc)
            .toSet()

        val putstatics = entryExitBlocks
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.PUTSTATIC && it.owner == owner }
            .filter { !profile.excludedFields.matches(it.owner, it.name, it.desc) }

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
        val done = mutableSetOf<DisjointSet.Partition<MemberRef>>()
        for ((partition, field) in fields) {
            spliceInitializers(done, partition, field)
        }
    }

    private fun spliceInitializers(
        done: MutableSet<DisjointSet.Partition<MemberRef>>,
        partition: DisjointSet.Partition<MemberRef>,
        field: Field
    ) {
        if (!done.add(partition)) {
            return
        }

        for (dependency in field.dependencies) {
            val dependencyPartition = inheritedFieldSets[dependency]!!
            val dependencyField = fields[dependencyPartition] ?: continue
            spliceInitializers(done, partition, dependencyField)
        }

        val (staticClass, clinit) = nextClass()
        staticClass.fields.add(field.node)
        staticClass.version = ClassVersionUtils.max(staticClass.version, field.version)
        clinit.instructions.insertBefore(clinit.instructions.last, field.initializer)
        clinit.maxStack = max(clinit.maxStack, field.maxStack)

        fieldClasses[partition] = staticClass.name
    }

    override fun preTransform(classPath: ClassPath) {
        inheritedFieldSets = classPath.createInheritedFieldSets()
        inheritedMethodSets = classPath.createInheritedMethodSets()
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

                val clinit = clazz.methods.find { it.name == "<clinit>" }
                val (simpleInitializers, complexInitializers) = clinit?.extractInitializers(clazz.name)
                    ?: Pair(emptyMap(), emptySet())

                clazz.fields.removeIf { field ->
                    if (field.access and Opcodes.ACC_STATIC == 0) {
                        return@removeIf false
                    } else if (profile.excludedFields.matches(clazz.name, field.name, field.desc)) {
                        return@removeIf false
                    }

                    val desc = MemberDesc(field)
                    if (complexInitializers.contains(desc)) {
                        return@removeIf false
                    }

                    val initializer = simpleInitializers[desc] ?: InsnList()
                    val maxStack = clinit?.maxStack ?: 0

                    val partition = inheritedFieldSets[MemberRef(clazz, field)]!!
                    fields[partition] = Field(field, initializer, clazz.version, maxStack)
                    return@removeIf true
                }

                clazz.methods.removeIf { method ->
                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        return@removeIf false
                    } else if (method.access and Opcodes.ACC_NATIVE != 0) {
                        return@removeIf false
                    } else if (profile.excludedMethods.matches(clazz.name, method.name, method.desc)) {
                        return@removeIf false
                    }

                    val (staticClass, _) = nextClass()
                    staticClass.methods.add(method)
                    staticClass.version = ClassVersionUtils.max(staticClass.version, clazz.version)

                    val partition = inheritedMethodSets[MemberRef(clazz, method)]!!
                    methodClasses[partition] = staticClass.name
                    return@removeIf true
                }

                val first = clinit?.instructions?.firstOrNull { it.opcode != -1 }
                if (first != null && first.opcode == Opcodes.RETURN) {
                    clazz.methods.remove(clinit)
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

    private companion object {
        private val logger = InlineLogger()
        private const val MAX_FIELDS = 500
        private const val MAX_METHODS = 50
    }
}
