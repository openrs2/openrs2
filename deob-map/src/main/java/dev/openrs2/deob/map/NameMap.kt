package dev.openrs2.deob.map

import dev.openrs2.asm.MemberRef
import java.util.SortedMap

data class NameMap(
    val classes: SortedMap<String, String>,
    val fields: SortedMap<MemberRef, Field>,
    val methods: SortedMap<MemberRef, Method>
)
