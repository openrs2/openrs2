package dev.openrs2.deob.remap

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.ExtendedRemapper
import dev.openrs2.asm.filter.UnionMemberFilter
import dev.openrs2.deob.Profile
import dev.openrs2.deob.filter.BrowserControlFilter
import dev.openrs2.deob.util.map.NameMap
import dev.openrs2.util.collect.DisjointSet
import org.objectweb.asm.tree.AbstractInsnNode

class TypedRemapper private constructor(
    private val inheritedFieldSets: DisjointSet<MemberRef>,
    private val inheritedMethodSets: DisjointSet<MemberRef>,
    private val classes: Map<String, String>,
    private val fields: Map<DisjointSet.Partition<MemberRef>, String>,
    private val methods: Map<DisjointSet.Partition<MemberRef>, String>,
    private val staticFields: Map<DisjointSet.Partition<MemberRef>, StaticField>,
    private val staticMethods: Map<DisjointSet.Partition<MemberRef>, String>
) : ExtendedRemapper() {
    override fun map(internalName: String): String {
        return classes.getOrDefault(internalName, internalName)
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedFieldSets[member] ?: return name
        return fields.getOrDefault(partition, name)
    }

    override fun mapFieldOwner(owner: String, name: String, descriptor: String): String {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedFieldSets[member] ?: return mapType(owner)
        return staticFields[partition]?.owner ?: mapType(owner)
    }

    override fun getFieldInitializer(owner: String, name: String, descriptor: String): List<AbstractInsnNode>? {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedFieldSets[member] ?: return null
        return staticFields[partition]?.initializer
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedMethodSets[member] ?: return name
        return methods.getOrDefault(partition, name)
    }

    override fun mapMethodOwner(owner: String, name: String, descriptor: String): String {
        val member = MemberRef(owner, name, descriptor)
        val partition = inheritedMethodSets[member] ?: return mapType(owner)
        return staticMethods.getOrDefault(partition, mapType(owner))
    }

    companion object {
        private val logger = InlineLogger()

        fun create(classPath: ClassPath, profile: Profile, nameMap: NameMap): TypedRemapper {
            val inheritedFieldSets = classPath.createInheritedFieldSets()
            val inheritedMethodSets = classPath.createInheritedMethodSets()

            val classes = ClassMappingGenerator(
                classPath,
                profile.excludedClasses,
                nameMap
            ).generate()
            val fields = FieldMappingGenerator(
                classPath,
                profile.excludedFields,
                nameMap,
                inheritedFieldSets,
                classes
            ).generate()
            val methods = MethodMappingGenerator(
                classPath,
                profile.excludedMethods,
                nameMap,
                inheritedMethodSets
            ).generate()

            verifyMapping(classes, profile.maxObfuscatedNameLen)
            verifyMemberMapping(fields, profile.maxObfuscatedNameLen)
            verifyMemberMapping(methods, profile.maxObfuscatedNameLen)

            val browserControlFilter = BrowserControlFilter.create(classPath)

            val staticClassNameGenerator = NameGenerator()
            val staticFields = StaticFieldUnscrambler(
                classPath,
                UnionMemberFilter(profile.excludedFields, browserControlFilter),
                profile.scrambledLibraries,
                nameMap,
                inheritedFieldSets,
                staticClassNameGenerator
            ).unscramble()
            val staticMethods = StaticMethodUnscrambler(
                classPath,
                UnionMemberFilter(profile.excludedMethods, browserControlFilter),
                profile.scrambledLibraries,
                nameMap,
                inheritedMethodSets,
                staticClassNameGenerator
            ).unscramble()

            return TypedRemapper(
                inheritedFieldSets,
                inheritedMethodSets,
                classes,
                fields,
                methods,
                staticFields,
                staticMethods
            )
        }

        private fun verifyMapping(mapping: Map<String, String>, maxObfuscatedNameLen: Int) {
            for ((key, value) in mapping) {
                verifyMapping(key, value, maxObfuscatedNameLen)
            }
        }

        private fun verifyMemberMapping(
            mapping: Map<DisjointSet.Partition<MemberRef>, String>,
            maxObfuscatedNameLen: Int
        ) {
            for ((key, value) in mapping) {
                verifyMapping(key.first().name, value, maxObfuscatedNameLen)
            }
        }

        private fun verifyMapping(name: String, mappedName: String, maxObfuscatedNameLen: Int) {
            val originalName = StripClassNamePrefixRemapper.map(name)
            if (originalName.length > maxObfuscatedNameLen && originalName != mappedName) {
                logger.warn { "Remapping probably unobfuscated name $originalName to $mappedName" }
            }
        }
    }
}
