package dev.openrs2.asm.filter

public object AnyClassFilter : ClassFilter {
    override fun matches(name: String): Boolean {
        return true
    }
}
