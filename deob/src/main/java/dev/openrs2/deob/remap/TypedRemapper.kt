package dev.openrs2.deob.remap

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassMetadata
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.util.collect.DisjointSet
import dev.openrs2.util.indefiniteArticle
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.commons.Remapper

class TypedRemapper private constructor(
    private val classes: Map<String, String>,
    private val fields: Map<MemberRef, String>,
    private val methods: Map<MemberRef, String>
) : Remapper() {
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

        val EXCLUDED_CLASSES = setOf(
            "client",
            "jagex3/jagmisc/jagmisc",
            "loader",
            "unpack",
            "unpackclass"
        )
        val EXCLUDED_METHODS = setOf(
            "<clinit>",
            "<init>",
            "main",
            "providesignlink",
            "quit"
        )
        val EXCLUDED_FIELDS = setOf(
            "cache"
        )

        private const val MAX_OBFUSCATED_NAME_LEN = 2

        fun create(classPath: ClassPath): TypedRemapper {
            val inheritedFieldSets = classPath.createInheritedFieldSets()
            val inheritedMethodSets = classPath.createInheritedMethodSets()

            val classes = createClassMapping(classPath.libraryClasses)
            val fields = createFieldMapping(classPath, inheritedFieldSets, classes)
            val methods = createMethodMapping(classPath, inheritedMethodSets)

            verifyMapping(classes)
            verifyMemberMapping(fields)
            verifyMemberMapping(methods)

            return TypedRemapper(classes, fields, methods)
        }

        private fun verifyMapping(mapping: Map<String, String>) {
            for ((key, value) in mapping) {
                verifyMapping(key, value)
            }
        }

        private fun verifyMemberMapping(mapping: Map<MemberRef, String>) {
            for ((key, value) in mapping) {
                verifyMapping(key.name, value)
            }
        }

        private fun verifyMapping(name: String, mappedName: String) {
            val originalName = name.replace("^(?:loader|unpackclass)_".toRegex(), "")
            if (originalName.length > MAX_OBFUSCATED_NAME_LEN && originalName != mappedName) {
                logger.warn { "Remapping probably unobfuscated name $originalName to $mappedName" }
            }
        }

        private fun generateName(prefixes: MutableMap<String, Int>, prefix: String): String {
            val separator = if (prefix.last().isDigit()) {
                "_"
            } else {
                ""
            }
            return prefix + separator + prefixes.merge(prefix, 1, Integer::sum)
        }

        private fun createClassMapping(classes: List<ClassMetadata>): Map<String, String> {
            val mapping = mutableMapOf<String, String>()
            val prefixes = mutableMapOf<String, Int>()
            for (clazz in classes) {
                populateClassMapping(mapping, prefixes, clazz)
            }
            return mapping
        }

        private fun populateClassMapping(
            mapping: MutableMap<String, String>,
            prefixes: MutableMap<String, Int>,
            clazz: ClassMetadata
        ): String {
            val name = clazz.name
            if (mapping.containsKey(name) || !isClassRenamable(clazz)) {
                return mapping.getOrDefault(name, name)
            }

            val mappedName = generateClassName(mapping, prefixes, clazz)
            mapping[name] = mappedName
            return mappedName
        }

        private fun isClassRenamable(clazz: ClassMetadata): Boolean {
            if (clazz.name in EXCLUDED_CLASSES || clazz.dependency) {
                return false
            }

            for (method in clazz.methods) {
                if (clazz.getMethodAccess(method)!! and Opcodes.ACC_NATIVE != 0) {
                    return false
                }
            }

            return true
        }

        private fun generateClassName(
            mapping: MutableMap<String, String>,
            prefixes: MutableMap<String, Int>,
            clazz: ClassMetadata
        ): String {
            val name = clazz.name
            var mappedName = name.substring(0, name.lastIndexOf('/') + 1)

            val superClass = clazz.superClass
            if (superClass != null && superClass.name != "java/lang/Object") {
                var superName = populateClassMapping(mapping, prefixes, superClass)
                superName = superName.substring(superName.lastIndexOf('/') + 1)
                mappedName += generateName(prefixes, superName + "_Sub")
            } else if (clazz.`interface`) {
                mappedName += generateName(prefixes, "Interface")
            } else {
                mappedName += generateName(prefixes, "Class")
            }

            return mappedName
        }

        private fun createFieldMapping(
            classPath: ClassPath,
            disjointSet: DisjointSet<MemberRef>,
            classMapping: Map<String, String>
        ): Map<MemberRef, String> {
            val mapping = mutableMapOf<MemberRef, String>()
            val prefixes = mutableMapOf<String, Int>()

            for (partition in disjointSet) {
                if (!isFieldRenamable(classPath, partition)) {
                    continue
                }

                val type = Type.getType(partition.first().desc)
                val mappedName = generateFieldName(prefixes, classMapping, type)
                for (field in partition) {
                    mapping[field] = mappedName
                }
            }

            return mapping
        }

        private fun isFieldRenamable(classPath: ClassPath, partition: DisjointSet.Partition<MemberRef>): Boolean {
            for (field in partition) {
                val clazz = classPath[field.owner]!!

                if (field.name in EXCLUDED_FIELDS || clazz.dependency) {
                    return false
                }
            }

            return true
        }

        private fun generateFieldName(
            prefixes: MutableMap<String, Int>,
            classMapping: Map<String, String>,
            type: Type
        ): String {
            val dimensions: String
            val elementType: Type
            if (type.sort == Type.ARRAY) {
                dimensions = "Array".repeat(type.dimensions)
                elementType = type.elementType
            } else {
                dimensions = ""
                elementType = type
            }

            val prefix = when (elementType.sort) {
                Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT, Type.LONG, Type.FLOAT, Type.DOUBLE -> {
                    elementType.className + dimensions
                }
                Type.OBJECT -> {
                    val className = classMapping.getOrDefault(elementType.internalName, elementType.internalName)
                    className.substring(className.lastIndexOf('/') + 1) + dimensions
                }
                else -> throw IllegalArgumentException("Unknown field type $elementType")
            }

            return generateName(prefixes, prefix.indefiniteArticle() + prefix.capitalize())
        }

        private fun createMethodMapping(
            classPath: ClassPath,
            disjointSet: DisjointSet<MemberRef>
        ): Map<MemberRef, String> {
            val mapping = mutableMapOf<MemberRef, String>()
            var id = 0

            for (partition in disjointSet) {
                if (!isMethodRenamable(classPath, partition)) {
                    continue
                }

                val mappedName = "method" + ++id
                for (method in partition) {
                    mapping[method] = mappedName
                }
            }

            return mapping
        }

        fun isMethodRenamable(classPath: ClassPath, partition: DisjointSet.Partition<MemberRef>): Boolean {
            for (method in partition) {
                val clazz = classPath[method.owner]!!

                if (method.name in EXCLUDED_METHODS || clazz.dependency) {
                    return false
                }

                val access = clazz.getMethodAccess(MemberDesc(method))
                if (access != null && access and Opcodes.ACC_NATIVE != 0) {
                    return false
                }
            }

            return true
        }
    }
}
