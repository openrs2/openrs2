package org.openrs2.deob.bytecode.remap

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.MemberDesc
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.asm.getExpression
import org.openrs2.asm.isSequential
import org.openrs2.deob.util.map.NameMap
import org.openrs2.util.collect.DisjointSet

public class StaticFieldUnscrambler(
    private val classPath: ClassPath,
    private val excludedFields: MemberFilter,
    private val scrambledLibraries: Set<String>,
    private val nameMap: NameMap,
    private val inheritedFieldSets: DisjointSet<MemberRef>,
    private val staticClassMapping: StaticClassMapping
) {
    public fun unscramble(): Map<DisjointSet.Partition<MemberRef>, StaticField> {
        val fields = mutableMapOf<DisjointSet.Partition<MemberRef>, StaticField>()

        for (library in classPath.libraries) {
            if (library.name !in scrambledLibraries) {
                continue
            }

            for (clazz in library) {
                if (clazz.name.contains('/')) {
                    continue
                }

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
                    val owner = nameMap.mapFieldOwner(partition, staticClassMapping[clazz.name])
                    fields[partition] = StaticField("${library.name}!$owner", simpleInitializers[desc])
                }
            }
        }

        return fields
    }

    private fun MethodNode.extractEntryExitBlocks(): Set<AbstractInsnNode> {
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
            return entry.toSet()
        }

        val exit = instructions.toList()
            .dropLast(1)
            .takeLastWhile { it.isSequential }

        return (entry + exit).toSet()
    }

    private fun MethodNode.extractInitializers(
        owner: String
    ): Pair<Map<MemberDesc, List<AbstractInsnNode>>, Set<MemberDesc>> {
        val entryExitBlocks = extractEntryExitBlocks()

        val simpleInitializers = mutableMapOf<MemberDesc, List<AbstractInsnNode>>()
        val complexInitializers = instructions.asSequence()
            .filter { it !in entryExitBlocks }
            .filterIsInstance<FieldInsnNode>()
            .filter { it.owner == owner }
            .map(::MemberDesc)
            .toMutableSet()

        val putstatics = entryExitBlocks
            .filterIsInstance<FieldInsnNode>()
            .filter { it.opcode == Opcodes.PUTSTATIC && it.owner == owner }

        for (putstatic in putstatics) {
            val desc = MemberDesc(putstatic)
            if (desc in complexInitializers) {
                continue
            }

            if (desc in simpleInitializers) {
                simpleInitializers -= desc
                complexInitializers += desc
                continue
            }

            // TODO(gpe): use a filter here (pure with no *LOADs?)
            val initializer = getExpression(putstatic)
            if (initializer != null) {
                simpleInitializers[desc] = initializer
            } else {
                complexInitializers += desc
            }
        }

        val overlappingInitializers = mutableSetOf<MemberDesc>()

        for ((field1, initializer1) in simpleInitializers) {
            for ((field2, initializer2) in simpleInitializers) {
                if (field1 != field2 && initializer1.any { it in initializer2 }) {
                    overlappingInitializers += field1
                }
            }
        }

        simpleInitializers -= overlappingInitializers
        complexInitializers += overlappingInitializers

        return Pair(simpleInitializers, complexInitializers)
    }
}
