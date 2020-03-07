package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import com.google.common.collect.HashMultimap
import com.google.common.collect.Multimap
import dev.openrs2.asm.ClassForNameUtils
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.common.collect.DisjointSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class VisibilityTransformer : Transformer() {
    private lateinit var inheritedFieldSets: DisjointSet<MemberRef>
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private val fieldReferences = HashMultimap.create<DisjointSet.Partition<MemberRef>, String>()
    private val methodReferences = HashMultimap.create<DisjointSet.Partition<MemberRef>, String>()
    private val publicCtorClasses = mutableSetOf<String>()

    override fun preTransform(classPath: ClassPath) {
        inheritedFieldSets = classPath.createInheritedFieldSets()
        inheritedMethodSets = classPath.createInheritedMethodSets()
        fieldReferences.clear()
        methodReferences.clear()
        publicCtorClasses.clear()
        publicCtorClasses.addAll(DEFAULT_PUBLIC_CTOR_CLASSES)
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (name in ClassForNameUtils.findClassNames(method)) {
            val loadedClass = classPath[name]
            if (loadedClass != null && !loadedClass.dependency) {
                publicCtorClasses.add(name)
            }
        }

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
        classAccess: Int,
        access: Int
    ): Int {
        if (classAccess and Opcodes.ACC_INTERFACE != 0) {
            return Opcodes.ACC_PUBLIC
        }

        val method = Type.getType(member.desc).sort == Type.METHOD
        if (method) {
            if (member.name == "<clinit>") {
                // the visibility flags don't really matter - we use package-private to match javac
                return 0
            } else if (member.owner in publicCtorClasses && member.name == "<init>") {
                // constructors invoked with reflection (including applets) must be public
                return Opcodes.ACC_PUBLIC
            } else if (member.name in PUBLIC_METHODS) {
                // methods invoked with reflection must also be public
                return Opcodes.ACC_PUBLIC
            }
        }

        val partition = disjointSet[member]!!

        val overridable = method && member.name != "<init>"
        val hasOverride = overridable && partition.count { classPath[it.owner]!!.methods.contains(MemberDesc(it)) } > 1
        val abstract = method && access and Opcodes.ACC_ABSTRACT != 0
        val partitionReferences = references[partition]
        val partitionOwners = partition.mapTo(mutableSetOf(), MemberRef::owner)

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
        logger.info { "Identified constructors invoked with reflection $publicCtorClasses" }

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
                        clazz.access,
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
                        clazz.access,
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

    companion object {
        private val logger = InlineLogger()
        private const val VISIBILITY_FLAGS = Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE
        private val DEFAULT_PUBLIC_CTOR_CLASSES = setOf("client", "loader", "unpackclass")
        private val PUBLIC_METHODS = setOf("main", "providesignlink")

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
