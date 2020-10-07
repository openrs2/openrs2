package org.openrs2.deob.util.map

import org.openrs2.asm.MemberRef
import org.openrs2.util.collect.DisjointSet
import java.util.SortedMap
import java.util.TreeMap

public data class NameMap(
    val classes: SortedMap<String, String>,
    val fields: SortedMap<MemberRef, Field>,
    val methods: SortedMap<MemberRef, Method>
) {
    public constructor() : this(TreeMap(), TreeMap(), TreeMap())

    public fun add(other: NameMap) {
        classes.putAll(other.classes)
        fields.putAll(other.fields)
        methods.putAll(other.methods)
    }

    public fun mapClassName(name: String, default: String): String {
        return classes.getOrDefault(name, default)
    }

    public fun mapFieldName(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val field = fields[member]
            if (field != null) {
                return field.name
            }
        }

        return default
    }

    public fun mapFieldOwner(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val field = fields[member]
            if (field != null) {
                return field.owner
            }
        }

        return default
    }

    public fun mapMethodName(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val method = methods[member]
            if (method != null) {
                return method.name
            }
        }

        return default
    }

    public fun mapMethodOwner(partition: DisjointSet.Partition<MemberRef>, default: String): String {
        for (member in partition) {
            val method = methods[member]
            if (method != null) {
                return method.owner
            }
        }

        return default
    }
}
