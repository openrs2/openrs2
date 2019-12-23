package dev.openrs2.asm.classpath

import com.google.common.collect.ImmutableList
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.util.collect.DisjointSet
import dev.openrs2.util.collect.ForestDisjointSet
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.ClassNode

class ClassPath(
    private val runtime: ClassLoader,
    private val dependencies: ImmutableList<Library>,
    val libraries: List<Library>
) {
    private val cache = mutableMapOf<String, ClassMetadata>()

    val libraryClasses: List<ClassMetadata>
        get() {
            val classes = mutableListOf<ClassMetadata>()
            for (library in libraries) {
                for (clazz in library) {
                    classes.add(get(clazz.name))
                }
            }
            return classes
        }

    operator fun get(name: String): ClassMetadata = cache.computeIfAbsent(name) {
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

        val reflectionName = name.replace('/', '.')

        val clazz = try {
            runtime.loadClass(reflectionName)
        } catch (ex: ClassNotFoundException) {
            throw IllegalArgumentException("Unknown class $name")
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
                populateInheritedFieldSets(ancestorCache, disjointSet, get(clazz.name))
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
                val partition1 = disjointSet.add(MemberRef(clazz.name, field))
                val partition2 = disjointSet.add(MemberRef(superClass.name, field))
                disjointSet.union(partition1, partition2)
            }
            ancestorsBuilder.addAll(fields)
        }

        for (superInterface in clazz.superInterfaces) {
            val fields = populateInheritedFieldSets(ancestorCache, disjointSet, superInterface)
            for (field in fields) {
                val partition1 = disjointSet.add(MemberRef(clazz.name, field))
                val partition2 = disjointSet.add(MemberRef(superInterface.name, field))
                disjointSet.union(partition1, partition2)
            }
            ancestorsBuilder.addAll(fields)
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
                populateInheritedMethodSets(ancestorCache, disjointSet, get(clazz.name))
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
                val partition1 = disjointSet.add(MemberRef(clazz.name, method))
                val partition2 = disjointSet.add(MemberRef(superClass.name, method))
                disjointSet.union(partition1, partition2)
            }
            ancestorsBuilder.addAll(methods)
        }

        for (superInterface in clazz.superInterfaces) {
            val methods = populateInheritedMethodSets(ancestorCache, disjointSet, superInterface)
            for (method in methods) {
                val partition1 = disjointSet.add(MemberRef(clazz.name, method))
                val partition2 = disjointSet.add(MemberRef(superInterface.name, method))
                disjointSet.union(partition1, partition2)
            }
            ancestorsBuilder.addAll(methods)
        }

        for (method in clazz.methods) {
            disjointSet.add(MemberRef(clazz.name, method))
            ancestorsBuilder.add(method)
        }

        ancestorCache[clazz] = ancestorsBuilder
        return ancestorsBuilder
    }
}
