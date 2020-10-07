package org.openrs2.asm.filter

import com.fasterxml.jackson.annotation.JsonIgnore
import org.openrs2.asm.MemberRef

public class GlobMemberFilter(@Suppress("CanBeParameter") private val patterns: List<MemberRef>) : MemberFilter {
    private data class CompiledPattern(val owner: Regex, val name: Regex, val desc: Regex)

    @JsonIgnore
    private val compiledPatterns = patterns.map(::compile).toList()

    override fun matches(owner: String, name: String, desc: String): Boolean {
        return compiledPatterns.any { it.owner.matches(owner) && it.name.matches(name) && it.desc.matches(desc) }
    }

    private companion object {
        private fun compile(member: MemberRef): CompiledPattern {
            return CompiledPattern(
                Glob.compileClass(member.owner),
                Glob.compile(member.name),
                Glob.compile(member.desc)
            )
        }
    }
}
