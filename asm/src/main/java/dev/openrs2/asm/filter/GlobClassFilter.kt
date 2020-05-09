package dev.openrs2.asm.filter

import com.fasterxml.jackson.annotation.JsonIgnore

class GlobClassFilter(@Suppress("CanBeParameter") private val patterns: List<String>) : ClassFilter {
    @JsonIgnore
    private val compiledPatterns = patterns.map(Glob::compileClass).toList()

    override fun matches(name: String): Boolean {
        return compiledPatterns.any { it.matches(name) }
    }
}
