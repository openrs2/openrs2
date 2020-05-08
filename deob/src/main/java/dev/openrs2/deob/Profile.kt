package dev.openrs2.deob

import dev.openrs2.asm.filter.ClassFilter
import dev.openrs2.asm.filter.GlobClassFilter
import dev.openrs2.asm.filter.GlobMemberFilter
import dev.openrs2.asm.filter.MemberFilter

class Profile(
    val excludedClasses: ClassFilter,
    val excludedMethods: MemberFilter,
    val excludedFields: MemberFilter,
    val entryPoints: MemberFilter,
    val maxObfuscatedNameLen: Int
) {
    companion object {
        val BUILD_550 = Profile(
            excludedClasses = GlobClassFilter(
                "client",
                "jagex3/jagmisc/jagmisc",
                "loader",
                "unpack",
                "unpackclass"
            ),
            excludedMethods = GlobMemberFilter(
                "**.<clinit> *",
                "**.<init> *",
                "**.main *",
                "**.providesignlink *",
                "**.quit *"
            ),
            excludedFields = GlobMemberFilter(
                "**.cache *"
            ),
            entryPoints = GlobMemberFilter(
                "**.<clinit> *",
                "**.main *",
                "**.providesignlink *",
                "client.<init> *",
                "loader.<init> *",
                "unpackclass.<init> *"
            ),
            maxObfuscatedNameLen = 2
        )
    }
}
