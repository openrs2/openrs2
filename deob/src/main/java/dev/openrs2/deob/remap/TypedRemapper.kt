package dev.openrs2.deob.remap

import com.github.michaelbull.logging.InlineLogger
import com.google.common.base.Strings
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassMetadata
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.common.collect.DisjointSet
import dev.openrs2.common.indefiniteArticle
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
        private val EXCLUDED_METHODS = setOf(
            "<clinit>",
            "<init>",
            "main",
            "providesignlink",
            "quit"
        )
        private val EXCLUDED_FIELDS = setOf(
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
            val originalName = name.replace("^(?:loader|unpacker)_".toRegex(), "")
            if (originalName.length > MAX_OBFUSCATED_NAME_LEN && originalName != mappedName) {
                logger.warn { "Remapping probably unobfuscated name $originalName to $mappedName" }
            }
        }

        private fun generateName(prefixes: MutableMap<String, Int>, prefix: String): String {
            return prefix + prefixes.merge(prefix, 1, Integer::sum)
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
            if (mapping.containsKey(name) || EXCLUDED_CLASSES.contains(name) || clazz.dependency) {
                return mapping.getOrDefault(name, name)
            }

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

            mapping[name] = mappedName
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
                var skip = false

                for ((owner, name) in partition) {
                    val clazz = classPath[owner]

                    if (EXCLUDED_FIELDS.contains(name)) {
                        skip = true
                        break
                    }

                    if (clazz.dependency) {
                        skip = true
                        break
                    }
                }

                if (skip) {
                    continue
                }

                var prefix = ""

                var type = Type.getType(partition.iterator().next().desc)
                if (type.sort == Type.ARRAY) {
                    prefix = Strings.repeat("Array", type.dimensions)
                    type = type.elementType
                }

                when (type.sort) {
                    Type.BOOLEAN, Type.BYTE, Type.CHAR, Type.SHORT, Type.INT, Type.LONG, Type.FLOAT, Type.DOUBLE -> {
                        prefix = type.className + prefix
                    }
                    Type.OBJECT -> {
                        var className = classMapping.getOrDefault(type.internalName, type.internalName)
                        className = className.substring(className.lastIndexOf('/') + 1)
                        prefix = className + prefix
                    }
                    else -> throw IllegalArgumentException("Unknown field type $type")
                }

                prefix = prefix.indefiniteArticle() + prefix.capitalize()

                val mappedName = generateName(prefixes, prefix)
                for (field in partition) {
                    mapping[field] = mappedName
                }
            }

            return mapping
        }

        fun isMethodImmutable(classPath: ClassPath, partition: DisjointSet.Partition<MemberRef>): Boolean {
            for (method in partition) {
                val clazz = classPath[method.owner]

                if (EXCLUDED_METHODS.contains(method.name)) {
                    return true
                }

                if (clazz.dependency) {
                    return true
                }

                if (clazz.isNative(MemberDesc(method))) {
                    return true
                }
            }

            return false
        }

        private fun createMethodMapping(
            classPath: ClassPath,
            disjointSet: DisjointSet<MemberRef>
        ): Map<MemberRef, String> {
            val mapping = mutableMapOf<MemberRef, String>()
            var id = 0

            for (partition in disjointSet) {
                if (isMethodImmutable(classPath, partition)) {
                    continue
                }

                val mappedName = "method" + ++id
                for (method in partition) {
                    mapping[method] = mappedName
                }
            }

            return mapping
        }
    }
}
