package dev.openrs2.deob.remap

import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.filter.MemberFilter
import dev.openrs2.deob.util.map.NameMap
import dev.openrs2.util.collect.DisjointSet
import dev.openrs2.util.indefiniteArticle
import org.objectweb.asm.Type

public class FieldMappingGenerator(
    private val classPath: ClassPath,
    private val excludedFields: MemberFilter,
    private val nameMap: NameMap,
    private val inheritedFieldSets: DisjointSet<MemberRef>,
    private val classMapping: Map<String, String>
) {
    private val nameGenerator = NameGenerator()

    public fun generate(): Map<DisjointSet.Partition<MemberRef>, String> {
        val mapping = mutableMapOf<DisjointSet.Partition<MemberRef>, String>()

        for (partition in inheritedFieldSets) {
            if (!isRenamable(partition)) {
                continue
            }

            val type = Type.getType(partition.first().desc)
            mapping[partition] = nameMap.mapFieldName(partition, generateName(type))
        }

        return mapping
    }

    private fun isRenamable(partition: DisjointSet.Partition<MemberRef>): Boolean {
        for (field in partition) {
            val clazz = classPath[field.owner]!!

            if (excludedFields.matches(field) || clazz.dependency) {
                return false
            }
        }

        return true
    }

    private fun generateName(type: Type): String {
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
                className.getClassName() + dimensions
            }
            else -> throw IllegalArgumentException("Unknown field type $elementType")
        }

        return nameGenerator.generate(prefix.indefiniteArticle() + prefix.capitalize())
    }
}
