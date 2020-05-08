package dev.openrs2.asm.filter

interface ClassFilter {
    fun matches(name: String): Boolean
}
