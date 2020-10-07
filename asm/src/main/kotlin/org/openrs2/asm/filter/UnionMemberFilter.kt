package org.openrs2.asm.filter

public class UnionMemberFilter(vararg filters: MemberFilter) : MemberFilter {
    private val filters = filters.toList()

    override fun matches(owner: String, name: String, desc: String): Boolean {
        return filters.any { it.matches(owner, name, desc) }
    }
}
