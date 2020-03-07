package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.toBinaryClassName
import dev.openrs2.common.collect.DisjointSet
import dev.openrs2.common.collect.ForestDisjointSet
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

    fun getNode(name: String): ClassNode? {
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
        val disjointSet = ForestDisjointSet<MemberRef>()
        val ancestorCache = mutableMapOf<ClassMetadata, Set<MemberDesc>>()

        for (library in libraries) {
            for (clazz in library) {
                populateInheritedFieldSets(ancestorCache, disjointSet, get(clazz.name)!!)
            }
        }

        return disjointSet
    }

    private fun populateInheritedFieldSets(
        ancestorCache: MutableMap<ClassMetadata, Set<MemberDesc>>,
        disjointSet: DisjointSet<MemberRef>,
        clazz: ClassMetadata
    ): Set<MemberDesc> {
        val ancestors = ancestorCache[clazz]
        if (ancestors != null) {
            return ancestors
        }

        val ancestorsBuilder = mutableSetOf<MemberDesc>()

        val superClass = clazz.superClass
        if (superClass != null) {
            val fields = populateInheritedFieldSets(ancestorCache, disjointSet, superClass)
            for (field in fields) {
                val access = clazz.getFieldAccess(field)
                if (access != null && access and Opcodes.ACC_STATIC != 0) {
                    continue
                }

                val partition1 = disjointSet.add(MemberRef(clazz.name, field))
                val partition2 = disjointSet.add(MemberRef(superClass.name, field))
                disjointSet.union(partition1, partition2)

                ancestorsBuilder.add(field)
            }
        }

        for (superInterface in clazz.superInterfaces) {
            val fields = populateInheritedFieldSets(ancestorCache, disjointSet, superInterface)
            for (field in fields) {
                val access = clazz.getFieldAccess(field)
                if (access != null && access and Opcodes.ACC_STATIC != 0) {
                    continue
                }

                val partition1 = disjointSet.add(MemberRef(clazz.name, field))
                val partition2 = disjointSet.add(MemberRef(superInterface.name, field))
                disjointSet.union(partition1, partition2)

                ancestorsBuilder.add(field)
            }
        }

        for (field in clazz.fields) {
            disjointSet.add(MemberRef(clazz.name, field))
            ancestorsBuilder.add(field)
        }

        ancestorCache[clazz] = ancestorsBuilder
        return ancestorsBuilder
    }

    fun createInheritedMethodSets(): DisjointSet<MemberRef> {
        val disjointSet = ForestDisjointSet<MemberRef>()
        val ancestorCache = mutableMapOf<ClassMetadata, Set<MemberDesc>>()

        for (library in libraries) {
            for (clazz in library) {
                populateInheritedMethodSets(ancestorCache, disjointSet, get(clazz.name)!!)
            }
        }

        return disjointSet
    }

    private fun populateInheritedMethodSets(
        ancestorCache: MutableMap<ClassMetadata, Set<MemberDesc>>,
        disjointSet: DisjointSet<MemberRef>,
        clazz: ClassMetadata
    ): Set<MemberDesc> {
        val ancestors = ancestorCache[clazz]
        if (ancestors != null) {
            return ancestors
        }

        val ancestorsBuilder = mutableSetOf<MemberDesc>()

        val superClass = clazz.superClass
        if (superClass != null) {
            val methods = populateInheritedMethodSets(ancestorCache, disjointSet, superClass)
            for (method in methods) {
                val access = clazz.getMethodAccess(method)
                if (access != null && access and Opcodes.ACC_STATIC != 0) {
                    continue
                }

                val partition1 = disjointSet.add(MemberRef(clazz.name, method))
                val partition2 = disjointSet.add(MemberRef(superClass.name, method))
                disjointSet.union(partition1, partition2)

                ancestorsBuilder.add(method)
            }
        }

        for (superInterface in clazz.superInterfaces) {
            val methods = populateInheritedMethodSets(ancestorCache, disjointSet, superInterface)
            for (method in methods) {
                val access = clazz.getMethodAccess(method)
                if (access != null && access and Opcodes.ACC_STATIC != 0) {
                    continue
                }

                val partition1 = disjointSet.add(MemberRef(clazz.name, method))
                val partition2 = disjointSet.add(MemberRef(superInterface.name, method))
                disjointSet.union(partition1, partition2)

                ancestorsBuilder.add(method)
            }
        }

        for (method in clazz.methods) {
            disjointSet.add(MemberRef(clazz.name, method))
            ancestorsBuilder.add(method)
        }

        ancestorCache[clazz] = ancestorsBuilder
        return ancestorsBuilder
    }
}
