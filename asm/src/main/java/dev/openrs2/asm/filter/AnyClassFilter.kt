package dev.openrs2.asm.filter

object AnyClassFilter : ClassFilter {
    override fun matches(name: String): Boolean {
        return true
    }
}
