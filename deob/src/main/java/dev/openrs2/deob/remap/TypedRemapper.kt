package dev.openrs2.deob.remap

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.ExtendedRemapper
import dev.openrs2.deob.Profile

class TypedRemapper private constructor(
    private val classes: Map<String, String>,
    private val fields: Map<MemberRef, String>,
    private val methods: Map<MemberRef, String>
) : ExtendedRemapper() {
    override fun map(internalName: String): String {
        return classes.getOrDefault(internalName, internalName)
    }

    override fun mapFieldName(owner: String, name: String, descriptor: String): String {
        return fields.getOrDefault(MemberRef(owner, name, descriptor), name)
    }

    override fun mapMethodName(owner: String, name: String, descriptor: String): String {
        return methods.getOrDefault(MemberRef(owner, name, descriptor), name)
    }

    companion object {
        private val logger = InlineLogger()

        private val LIBRARY_PREFIX_REGEX = Regex("^(?:loader|unpackclass)_")

        fun create(classPath: ClassPath, profile: Profile): TypedRemapper {
            val classes = ClassMappingGenerator(classPath, profile.excludedClasses).generate()
            val fields = FieldMappingGenerator(classPath, profile.excludedFields, classes).generate()
            val methods = MethodMappingGenerator(classPath, profile.excludedMethods).generate()

            verifyMapping(classes, profile.maxObfuscatedNameLen)
            verifyMemberMapping(fields, profile.maxObfuscatedNameLen)
            verifyMemberMapping(methods, profile.maxObfuscatedNameLen)

            return TypedRemapper(classes, fields, methods)
        }

        private fun verifyMapping(mapping: Map<String, String>, maxObfuscatedNameLen: Int) {
            for ((key, value) in mapping) {
                verifyMapping(key, value, maxObfuscatedNameLen)
            }
        }

        private fun verifyMemberMapping(mapping: Map<MemberRef, String>, maxObfuscatedNameLen: Int) {
            for ((key, value) in mapping) {
                verifyMapping(key.name, value, maxObfuscatedNameLen)
            }
        }

        private fun verifyMapping(name: String, mappedName: String, maxObfuscatedNameLen: Int) {
            val originalName = name.replace(LIBRARY_PREFIX_REGEX, "")
            if (originalName.length > maxObfuscatedNameLen && originalName != mappedName) {
                logger.warn { "Remapping probably unobfuscated name $originalName to $mappedName" }
            }
        }
    }
}
