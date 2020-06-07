package dev.openrs2.asm.filter

object AnyMemberFilter : MemberFilter {
    override fun matches(owner: String, name: String, desc: String): Boolean {
        return true
    }
}
