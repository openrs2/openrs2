package org.openrs2.deob.bytecode.remap

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.tree.AbstractInsnNode
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.ExtendedRemapper
import org.openrs2.deob.bytecode.ArgRef
import org.openrs2.deob.bytecode.Profile
import org.openrs2.deob.util.map.NameMap
import org.openrs2.util.collect.DisjointSet

public class TypedRemapper private constructor(
    private val inheritedFieldSets: DisjointSet<MemberRef>,
    private val inheritedMethodSets: DisjointSet<MemberRef>,
    private val classes: Map<String, String>,
    private val fields: Map<DisjointSet.Partition<MemberRef>, String>,
    private val methods: Map<DisjointSet.Partition<MemberRef>, String>,
    private val argumentNames: Map<ArgRef, String>,
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

    override fun mapArgumentName(
        owner: String,
        name: String,
        descriptor: String,
        index: Int,
        argumentName: String?
    ): String? {
        val argument = ArgRef(MemberRef(owner, name, descriptor), index)
        return argumentNames[argument] ?: argumentName
    }

    public companion object {
        private val logger = InlineLogger()

        public fun create(classPath: ClassPath, profile: Profile, nameMap: NameMap): TypedRemapper {
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
            val argumentNames = ArgumentMappingGenerator(nameMap).generate()

            verifyMapping(classes, profile.maxObfuscatedNameLen)
            verifyMemberMapping(fields, profile.maxObfuscatedNameLen)
            verifyMemberMapping(methods, profile.maxObfuscatedNameLen)

            val staticClassMapping = StaticClassMapping()
            val staticFields = StaticFieldUnscrambler(
                classPath,
                profile.excludedFields,
                profile.scrambledLibraries,
                nameMap,
                inheritedFieldSets,
                staticClassMapping
            ).unscramble()
            val staticMethods = StaticMethodUnscrambler(
                classPath,
                profile.excludedMethods,
                profile.scrambledLibraries,
                nameMap,
                inheritedMethodSets,
                staticClassMapping
            ).unscramble()

            return TypedRemapper(
                inheritedFieldSets,
                inheritedMethodSets,
                classes,
                fields,
                methods,
                argumentNames,
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
            val justThis = originalName.substringAfterLast('/')
            if (justThis.length > maxObfuscatedNameLen && originalName != mappedName) {
                logger.warn { "Remapping probably unobfuscated name $originalName to $mappedName" }
            }
        }
    }
}
