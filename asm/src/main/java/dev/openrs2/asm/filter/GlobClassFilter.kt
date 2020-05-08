package dev.openrs2.asm.filter

class GlobClassFilter(vararg patterns: String) : ClassFilter {
    private val patterns = patterns.map(Glob::compileClass).toList()

    override fun matches(name: String): Boolean {
        return patterns.any { it.matches(name) }
    }
}
