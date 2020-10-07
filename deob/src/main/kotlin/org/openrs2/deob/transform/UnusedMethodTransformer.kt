package org.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.asm.filter.UnionMemberFilter
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.Profile
import org.openrs2.deob.filter.ReflectedConstructorFilter
import org.openrs2.util.collect.DisjointSet
import org.openrs2.util.collect.removeFirst
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class UnusedMethodTransformer @Inject constructor(private val profile: Profile) : Transformer() {
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private lateinit var excludedMethods: MemberFilter
    private val methodReferences = HashMultimap.create<DisjointSet.Partition<MemberRef>, MemberRef>()

    override fun preTransform(classPath: ClassPath) {
        inheritedMethodSets = classPath.createInheritedMethodSets()
        excludedMethods = UnionMemberFilter(profile.entryPoints, ReflectedConstructorFilter.create(classPath))
        methodReferences.clear()
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn is MethodInsnNode) {
                addReference(methodReferences, inheritedMethodSets, MemberRef(insn), MemberRef(clazz, method))
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        var methodsRemoved = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                val methods = clazz.methods.iterator()

                for (method in methods) {
                    if (method.access and Opcodes.ACC_NATIVE != 0) {
                        continue
                    } else if (excludedMethods.matches(clazz.name, method.name, method.desc)) {
                        continue
                    }

                    val member = MemberRef(clazz, method)
                    val partition = inheritedMethodSets[member]!!

                    if (partition.any { classPath[it.owner]!!.dependency }) {
                        continue
                    }

                    val references = methodReferences[partition]
                    if (references.isEmpty() || references.size == 1 && references.first() == member) {
                        methods.remove()
                        methodsRemoved++

                        for (ref in partition) {
                            val owner = library[ref.owner]!!
                            owner.methods.removeFirst { it.name == ref.name && it.desc == ref.desc }
                        }
                    }
                }
            }
        }

        logger.info { "Removed $methodsRemoved unused methods" }
    }

    private companion object {
        private val logger = InlineLogger()

        private fun addReference(
            references: Multimap<DisjointSet.Partition<MemberRef>, MemberRef>,
            disjointSet: DisjointSet<MemberRef>,
            member: MemberRef,
            className: MemberRef
        ) {
            val partition = disjointSet[member] ?: return
            references.put(partition, className)
        }
    }
}
