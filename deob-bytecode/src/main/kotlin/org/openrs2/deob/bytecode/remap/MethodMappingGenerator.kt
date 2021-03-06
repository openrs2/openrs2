package org.openrs2.deob.bytecode.remap

import org.objectweb.asm.Opcodes
import org.openrs2.asm.MemberDesc
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.deob.util.map.NameMap
import org.openrs2.util.collect.DisjointSet

public class MethodMappingGenerator(
    private val classPath: ClassPath,
    private val excludedMethods: MemberFilter,
    private val nameMap: NameMap,
    private val inheritedMethodSets: DisjointSet<MemberRef>
) {
    private var index = 0

    public fun generate(): Map<DisjointSet.Partition<MemberRef>, String> {
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

    public companion object {
        @Deprecated("No replacement yet")
        public fun isRenamable(
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
