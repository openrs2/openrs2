package org.openrs2.asm.filter

public object AnyMemberFilter : MemberFilter {
    override fun matches(owner: String, name: String, desc: String): Boolean {
        return true
    }
}
