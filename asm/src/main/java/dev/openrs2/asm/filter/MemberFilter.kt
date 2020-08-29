package dev.openrs2.asm.filter

import dev.openrs2.asm.MemberRef

public interface MemberFilter {
    public fun matches(owner: String, name: String, desc: String): Boolean

    public fun matches(member: MemberRef): Boolean {
        return matches(member.owner, member.name, member.desc)
    }
}
