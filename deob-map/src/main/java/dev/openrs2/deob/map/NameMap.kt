package dev.openrs2.deob.map

import dev.openrs2.asm.MemberRef
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
}
