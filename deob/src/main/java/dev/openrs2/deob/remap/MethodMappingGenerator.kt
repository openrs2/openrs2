package dev.openrs2.deob.remap

import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.filter.MemberFilter
import dev.openrs2.deob.util.map.NameMap
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.Opcodes

class MethodMappingGenerator(
    private val classPath: ClassPath,
    private val excludedMethods: MemberFilter,
    private val nameMap: NameMap,
    private val inheritedMethodSets: DisjointSet<MemberRef>
) {
    private var index = 0

    fun generate(): Map<DisjointSet.Partition<MemberRef>, String> {
        val mapping = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()

        for (partition in inheritedMethodSets) {
            @Suppress("DEPRECATION")
            if (!isRenamable(classPath, excludedMethods, partition)) {
                continue
            }

            val generatedName = "method" + ++index
            mapping[partition] = nameMap.mapMethodName(partition, generatedName)
        }

        return mapping
    }

    companion object {
        @Deprecated("No replacement yet")
        fun isRenamable(
            classPath: ClassPath,
            excludedMethods: MemberFilter,
            partition: DisjointSet.Partition<MemberRef>
        ): Boolean {
            for (method in partition) {
                val clazz = classPath[method.owner]!!

                if (excludedMethods.matches(method) || clazz.dependency) {
                    return false
                }

                val access = clazz.getMethodAccess(MemberDesc(method))
                if (access != null && access and Opcodes.ACC_NATIVE != 0) {
                    return false
                }
            }

            return true
        }
    }
}
