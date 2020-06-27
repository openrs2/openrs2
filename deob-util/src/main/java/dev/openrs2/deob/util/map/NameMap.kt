package dev.openrs2.deob.util.map

import dev.openrs2.asm.MemberRef
import dev.openrs2.util.collect.DisjointSet
import java.util.SortedMap
import java.util.TreeMap

data class NameMap(
    val classes: SortedMap<String, String>,
    val fields: SortedMap<MemberRef, Field>,
    val methods: SortedMap<MemberRef, Method>
) {
    constructor() : this(TreeMap(), TreeMap(), TreeMap())

    fun add(other: NameMap) {
        classes.putAll(other.classes)
        fields.putAll(other.fields)
        methods.putAll(other.methods)
    }

    fun mapClassName(name: String, default: String): String {
        return classes.getOrDefault(name, default)
    }

    fun mapFieldName(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val field = fields[member]
            if (field != null) {
                return field.name
            }
        }

        return default
    }

    fun mapFieldOwner(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val field = fields[member]
            if (field != null) {
                return field.owner
            }
        }

        return default
    }

    fun mapMethodName(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val method = methods[member]
            if (method != null) {
                return method.name
            }
        }

        return default
    }

    fun mapMethodOwner(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val method = methods[member]
            if (method != null) {
                return method.owner
            }
        }

        return default
    }
}
