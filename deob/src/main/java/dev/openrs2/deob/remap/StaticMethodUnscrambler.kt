package dev.openrs2.deob.remap

import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.filter.MemberFilter
import dev.openrs2.deob.util.map.NameMap
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.Opcodes

class StaticMethodUnscrambler(
    private val classPath: ClassPath,
    private val excludedMethods: MemberFilter,
    private val scrambledLibraries: Set<String>,
    private val nameMap: NameMap,
    private val inheritedMethodSets: DisjointSet<MemberRef>,
    staticClassNameGenerator: NameGenerator
) {
    private val generator = StaticClassGenerator(staticClassNameGenerator, MAX_METHODS_PER_CLASS)

    fun unscramble(): Map<DisjointSet.Partition<MemberRef>, String> {
        val owners = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()

        for (library in classPath.libraries) {
            if (library.name !in scrambledLibraries) {
                continue
            }

            for (clazz in library) {
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
                    val owner = nameMap.mapMethodOwner(partition, generator.generate())
                    owners[partition] = "${library.name}!$owner"
                }
            }
        }

        return owners
    }

    private companion object {
        private const val MAX_METHODS_PER_CLASS = 50
    }
}
