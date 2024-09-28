package org.openrs2.deob.bytecode.remap

import org.objectweb.asm.Opcodes
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.deob.util.map.NameMap
import org.openrs2.util.collect.DisjointSet

public class StaticMethodUnscrambler(
    private val classPath: ClassPath,
    private val excludedMethods: MemberFilter,
    private val scrambledLibraries: Set<String>,
    private val nameMap: NameMap,
    private val inheritedMethodSets: DisjointSet<MemberRef>,
    private val staticClassMapping: StaticClassMapping
) {
    public fun unscramble(): Map<DisjointSet.Partition<MemberRef>, String> {
        val owners = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()

        for (library in classPath.libraries) {
            if (library.name !in scrambledLibraries) {
                continue
            }

            for (clazz in library) {
                if (clazz.name.contains('/')) {
                    continue
                }

                for (method in clazz.methods) {
                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        continue
                    } else if (method.access and Opcodes.ACC_NATIVE != 0) {
                        continue
                    } else if (excludedMethods.matches(clazz.name, method.name, method.desc)) {
                        continue
                    }

                    val member = MemberRef(clazz, method)
                    val partition = inheritedMethodSets[member]!!
                    val owner = nameMap.mapMethodOwner(partition, staticClassMapping[clazz.name])
                    owners[partition] = "${library.name}!$owner"
                }
            }
        }

        return owners
    }
}
