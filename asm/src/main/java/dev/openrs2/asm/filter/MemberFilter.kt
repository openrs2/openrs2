package dev.openrs2.asm.filter

import dev.openrs2.asm.MemberRef

interface MemberFilter {
    fun matches(owner: String, name: String, desc: String): Boolean

    fun matches(member: MemberRef): Boolean {
        return matches(member.owner, member.name, member.desc)
    }
}
