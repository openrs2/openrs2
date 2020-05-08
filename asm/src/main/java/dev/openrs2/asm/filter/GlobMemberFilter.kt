package dev.openrs2.asm.filter

import dev.openrs2.asm.MemberRef

class GlobMemberFilter(vararg patterns: String) : MemberFilter {
    private data class CompiledPattern(val owner: Regex, val name: Regex, val desc: Regex)

    private val patterns = patterns.map(::compile).toList()

    override fun matches(owner: String, name: String, desc: String): Boolean {
        return patterns.any { it.owner.matches(owner) && it.name.matches(name) && it.desc.matches(desc) }
    }

    companion object {
        private fun compile(pattern: String): CompiledPattern {
            val ref = MemberRef.fromString(pattern)
            return CompiledPattern(
                Glob.compileClass(ref.owner),
                Glob.compile(ref.name),
                Glob.compile(ref.desc)
            )
        }
    }
}
