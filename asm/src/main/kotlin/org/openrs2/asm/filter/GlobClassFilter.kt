package org.openrs2.asm.filter

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore

public class GlobClassFilter @JsonCreator constructor(@Suppress("CanBeParameter") private val patterns: List<String>) : ClassFilter {
    @JsonIgnore
    private val compiledPatterns = patterns.map(Glob::compileClass).toList()

    override fun matches(name: String): Boolean {
        return compiledPatterns.any { it.matches(name) }
    }
}
