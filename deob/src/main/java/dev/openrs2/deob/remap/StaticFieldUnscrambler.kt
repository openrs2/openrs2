package dev.openrs2.deob.remap

import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.filter.MemberFilter
import dev.openrs2.asm.getExpression
import dev.openrs2.asm.isSequential
import dev.openrs2.deob.util.map.NameMap
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

public class StaticFieldUnscrambler(
    private val classPath: ClassPath,
    private val excludedFields: MemberFilter,
    private val scrambledLibraries: Set<String>,
    private val nameMap: NameMap,
    private val inheritedFieldSets: DisjointSet<MemberRef>,
    staticClassNameGenerator: NameGenerator
) {
    private val generator = StaticClassGenerator(staticClassNameGenerator, MAX_FIELDS_PER_CLASS)

    public fun unscramble(): Map<DisjointSet.Partition<MemberRef>, StaticField> {
        val fields = mutableMapOf<DisjointSet.Partition<MemberRef>, StaticField>()

        for (library in classPath.libraries) {
            if (library.name !in scrambledLibraries) {
                continue
            }

            for (clazz in library) {
                val clinit = clazz.methods.find { it.name == "<clinit>" }
                val (simpleInitializers, complexInitializers) = clinit?.extractInitializers(clazz.name)
                    ?: Pair(emptyMap(), emptySet())

                for (field in clazz.fields) {
                    if (field.access and Opcodes.ACC_STATIC == 0) {
                        continue
                    } else if (excludedFields.matches(clazz.name, field.name, field.desc)) {
                        continue
                    }

                    val desc = MemberDesc(field)
                    if (complexInitializers.contains(desc)) {
                        continue
                    }

                    val member = MemberRef(clazz, field)
                    val partition = inheritedFieldSets[member]!!
                    val owner = nameMap.mapFieldOwner(partition, generator.generate())
                    fields[partition] = StaticField("${library.name}!$owner", simpleInitializers[desc])
                }
            }
        }

        return fields
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

    private fun MethodNode.extractInitializers(
        owner: String
    ): Pair<Map<MemberDesc, List<AbstractInsnNode>>, Set<MemberDesc>> {
        val entryExitBlocks = extractEntryExitBlocks()

        val simpleInitializers = mutableMapOf<MemberDesc, List<AbstractInsnNode>>()
        val complexInitializers = instructions.asSequence()
            .filter { !entryExitBlocks.contains(it) }
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.GETSTATIC && it.owner == owner }
            .filter { !excludedFields.matches(it.owner, it.name, it.desc) }
            .map(::MemberDesc)
            .toSet()

        val putstatics = entryExitBlocks
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.PUTSTATIC && it.owner == owner }
            .filter { !excludedFields.matches(it.owner, it.name, it.desc) }

        for (putstatic in putstatics) {
            val desc = MemberDesc(putstatic)
            if (simpleInitializers.containsKey(desc) || complexInitializers.contains(desc)) {
                continue
            }

            // TODO(gpe): use a filter here (pure with no *LOADs?)
            val expr = getExpression(putstatic) ?: continue
            simpleInitializers[desc] = expr.plus(putstatic)
        }

        return Pair(simpleInitializers, complexInitializers)
    }

    private companion object {
        private const val MAX_FIELDS_PER_CLASS = 500
    }
}
