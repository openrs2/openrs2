package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.toBinaryClassName
import dev.openrs2.util.collect.DisjointSet
import dev.openrs2.util.collect.ForestDisjointSet
import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode

class ClassPath(
    private val runtime: ClassLoader,
    private val dependencies: List<Library>,
    val libraries: List<Library>
) {
    private val cache = mutableMapOf<String, ClassMetadata?>()

    /*
     * XXX(gpe): this is a bit of a hack, as it makes the asm module contain
     * some details that are only relevant in the deobfuscator. However, I
     * can't think of a better way of storing this state at the moment - ASM
     * doesn't have support for attaching arbitrary state to an
     * AbstractInsnNode. We need to persist the state across all of our
     * Transformers to avoid adding extraneous labels until the last possible
     * moment, which would confuse some of our analyses if added earlier.
     */
    val originalPcs = mutableMapOf<AbstractInsnNode, Int>()

    val libraryClasses: List<ClassMetadata>
        get() {
            val classes = mutableListOf<ClassMetadata>()
            for (library in libraries) {
                for (clazz in library) {
                    classes.add(get(clazz.name)!!)
                }
            }
            return classes
        }

    private inline fun computeIfAbsent(name: String, f: (String) -> ClassMetadata?): ClassMetadata? {
        if (cache.containsKey(name)) {
            return cache[name]
        }

        val clazz = f(name)
        cache[name] = clazz
        return clazz
    }

    operator fun get(name: String): ClassMetadata? = computeIfAbsent(name) {
        for (library in libraries) {
            val clazz = library[name]
            if (clazz != null) {
                return@computeIfAbsent AsmClassMetadata(this, clazz, false)
            }
        }

        for (library in dependencies) {
            val clazz = library[name]
            if (clazz != null) {
                return@computeIfAbsent AsmClassMetadata(this, clazz, true)
            }
        }

        val clazz = try {
            runtime.loadClass(name.toBinaryClassName())
        } catch (ex: ClassNotFoundException) {
            return@computeIfAbsent null
        }

        return@computeIfAbsent ReflectionClassMetadata(this, clazz)
    }

    fun getClassNode(name: String): ClassNode? {
        for (library in libraries) {
            val clazz = library[name]
            if (clazz != null) {
                return clazz
            }
        }

        return null
    }

    fun remap(remapper: Remapper) {
        for (library in libraries) {
            library.remap(remapper)
        }

        cache.clear()
    }

    fun createInheritedFieldSets(): DisjointSet<MemberRef> {
        return createInheritedMemberSets(ClassMetadata::fields, ClassMetadata::getFieldAccess, fields = true)
    }

    fun createInheritedMethodSets(): DisjointSet<MemberRef> {
        return createInheritedMemberSets(ClassMetadata::methods, ClassMetadata::getMethodAccess, fields = false)
    }

    private fun createInheritedMemberSets(
        getMembers: (ClassMetadata) -> List<MemberDesc>,
        getMemberAccess: (ClassMetadata, MemberDesc) -> Int?,
        fields: Boolean
    ): DisjointSet<MemberRef> {
        val disjointSet = ForestDisjointSet<MemberRef>()
        val ancestorCache = mutableMapOf<ClassMetadata, Set<MemberDesc>>()

        for (library in libraries) {
            for (clazz in library) {
                populateInheritedMemberSets(
                    getMembers,
                    getMemberAccess,
                    fields,
                    ancestorCache,
                    disjointSet,
                    get(clazz.name)!!
                )
            }
        }

        return disjointSet
    }

    private fun populateInheritedMemberSets(
        getMembers: (ClassMetadata) -> List<MemberDesc>,
        getMemberAccess: (ClassMetadata, MemberDesc) -> Int?,
        fields: Boolean,
        ancestorCache: MutableMap<ClassMetadata, Set<MemberDesc>>,
        disjointSet: DisjointSet<MemberRef>,
        clazz: ClassMetadata
    ): Set<MemberDesc> {
        val ancestors = ancestorCache[clazz]
        if (ancestors != null) {
            return ancestors
        }

        val ancestorsBuilder = mutableSetOf<MemberDesc>()

        for (superClass in clazz.superClassAndInterfaces) {
            val members =
                populateInheritedMemberSets(getMembers, getMemberAccess, fields, ancestorCache, disjointSet, superClass)

            for (member in members) {
                val access = getMemberAccess(clazz, member)
                if (access != null && (access and Opcodes.ACC_STATIC != 0 || fields)) {
                    continue
                }

                val partition1 = disjointSet.add(MemberRef(clazz.name, member))
                val partition2 = disjointSet.add(MemberRef(superClass.name, member))
                disjointSet.union(partition1, partition2)

                ancestorsBuilder.add(member)
            }
        }

        for (member in getMembers(clazz)) {
            disjointSet.add(MemberRef(clazz.name, member))
            ancestorsBuilder.add(member)
        }

        ancestorCache[clazz] = ancestorsBuilder
        return ancestorsBuilder
    }
}
