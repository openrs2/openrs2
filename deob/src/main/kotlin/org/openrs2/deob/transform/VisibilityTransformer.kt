package org.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.MemberDesc
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.asm.filter.UnionMemberFilter
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.Profile
import org.openrs2.deob.filter.ReflectedConstructorFilter
import org.openrs2.util.collect.DisjointSet
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class VisibilityTransformer @Inject constructor(private val profile: Profile) : Transformer() {
    private lateinit var inheritedFieldSets: DisjointSet<MemberRef>
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private lateinit var entryPoints: MemberFilter
    private val fieldReferences = HashMultimap.create<DisjointSet.Partition<MemberRef>, String>()
    private val methodReferences = HashMultimap.create<DisjointSet.Partition<MemberRef>, String>()

    override fun preTransform(classPath: ClassPath) {
        inheritedFieldSets = classPath.createInheritedFieldSets()
        inheritedMethodSets = classPath.createInheritedMethodSets()
        entryPoints = UnionMemberFilter(profile.entryPoints, ReflectedConstructorFilter.create(classPath))
        fieldReferences.clear()
        methodReferences.clear()
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            when (insn) {
                is FieldInsnNode -> addReference(fieldReferences, inheritedFieldSets, MemberRef(insn), clazz.name)
                is MethodInsnNode -> addReference(methodReferences, inheritedMethodSets, MemberRef(insn), clazz.name)
            }
        }

        return false
    }

    private fun getVisibility(
        classPath: ClassPath,
        references: Multimap<DisjointSet.Partition<MemberRef>, String>,
        disjointSet: DisjointSet<MemberRef>,
        member: MemberRef,
        access: Int
    ): Int {
        val method = Type.getType(member.desc).sort == Type.METHOD
        if (method) {
            if (member.name == "<clinit>") {
                // the visibility flags don't really matter - we use package-private to match javac
                return 0
            } else if (entryPoints.matches(member)) {
                // entry points must be public
                return Opcodes.ACC_PUBLIC
            }
        }

        val partition = disjointSet[member]!!

        val overridable = method && member.name != "<init>"
        val hasOverride = overridable && partition.count { classPath[it.owner]!!.methods.contains(MemberDesc(it)) } > 1
        val abstract = method && access and Opcodes.ACC_ABSTRACT != 0
        val partitionReferences = references[partition]
        val partitionOwners = partition.mapTo(mutableSetOf(), MemberRef::owner)

        val declaredByInterface = partitionOwners.any { classPath[it]!!.`interface` }
        if (declaredByInterface) {
            return Opcodes.ACC_PUBLIC
        }

        // pick the weakest access level based on references in our own code
        val visibility = when {
            partitionReferences.all { it == member.owner } && !hasOverride && !abstract -> Opcodes.ACC_PRIVATE
            partitionReferences.all { partitionOwners.contains(it) } -> Opcodes.ACC_PROTECTED
            else -> Opcodes.ACC_PUBLIC
        }

        return if (overridable) {
            // reduce it to the weakest level required to override a dependency's method
            partition.filter { classPath[it.owner]!!.dependency }
                .mapNotNull { classPath[it.owner]!!.getMethodAccess(MemberDesc(it)) }
                .fold(visibility, ::weakestVisibility)
        } else {
            visibility
        }
    }

    override fun postTransform(classPath: ClassPath) {
        var classesChanged = 0
        var fieldsChanged = 0
        var methodsChanged = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                val classAccess = clazz.access
                clazz.access = (classAccess and VISIBILITY_FLAGS.inv()) or Opcodes.ACC_PUBLIC
                if (clazz.access != classAccess) {
                    classesChanged++
                }

                for (field in clazz.fields) {
                    val access = field.access

                    val visibility = getVisibility(
                        classPath,
                        fieldReferences,
                        inheritedFieldSets,
                        MemberRef(clazz, field),
                        access
                    )
                    field.access = (access and VISIBILITY_FLAGS.inv()) or visibility

                    if (field.access != access) {
                        fieldsChanged++
                    }
                }

                for (method in clazz.methods) {
                    val access = method.access

                    val visibility = getVisibility(
                        classPath,
                        methodReferences,
                        inheritedMethodSets,
                        MemberRef(clazz, method),
                        access
                    )
                    method.access = (access and VISIBILITY_FLAGS.inv()) or visibility

                    if (method.access != access) {
                        methodsChanged++
                    }
                }
            }
        }

        logger.info {
            "Updated visibility of $classesChanged classes, $fieldsChanged fields and $methodsChanged methods"
        }
    }

    private companion object {
        private val logger = InlineLogger()
        private const val VISIBILITY_FLAGS = Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE

        private fun addReference(
            references: Multimap<DisjointSet.Partition<MemberRef>, String>,
            disjointSet: DisjointSet<MemberRef>,
            member: MemberRef,
            className: String
        ) {
            val partition = disjointSet[member] ?: return
            references.put(partition, className)
        }

        private fun weakestVisibility(a: Int, b: Int): Int {
            return when {
                a and Opcodes.ACC_PUBLIC != 0 || b and Opcodes.ACC_PUBLIC != 0 -> Opcodes.ACC_PUBLIC
                // map package-private to public
                a and VISIBILITY_FLAGS == 0 || b and VISIBILITY_FLAGS == 0 -> Opcodes.ACC_PUBLIC
                a and Opcodes.ACC_PROTECTED != 0 || b and Opcodes.ACC_PROTECTED != 0 -> Opcodes.ACC_PROTECTED
                else -> Opcodes.ACC_PRIVATE
            }
        }
    }
}
