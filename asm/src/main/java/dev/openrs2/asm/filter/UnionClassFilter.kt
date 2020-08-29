package dev.openrs2.asm.filter

public class UnionClassFilter(vararg filters: ClassFilter) : ClassFilter {
    private val filters = filters.toList()

    override fun matches(name: String): Boolean {
        return filters.any { it.matches(name) }
    }
}
