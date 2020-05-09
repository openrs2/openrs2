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
                "com/sun/opengl/impl/x11/**",
                "jagex3/jagmisc/jagmisc",
                "jaggl/**",
                "javax/media/opengl/**",
                "loader",
                "unpack",
                "unpackclass"
            ),
            excludedMethods = GlobMemberFilter(
                "**.<clinit> *",
                "**.<init> *",
                "**.main *",
                "**.providesignlink *",
                "**.quit *",
                "com/sun/opengl/impl/x11/**.* *",
                "jaggl/**.* *",
                "javax/media/opengl/**.* *"
            ),
            excludedFields = GlobMemberFilter(
                "**.cache *",
                "com/sun/opengl/impl/x11/**.* *",
                "jaggl/**.* *",
                "javax/media/opengl/**.* *"
            ),
            entryPoints = GlobMemberFilter(
                "**.<clinit> *",
                "**.main *",
                "**.providesignlink *",
                "client.<init> *",
                "com/sun/opengl/impl/x11/DRIHack.begin *",
                "com/sun/opengl/impl/x11/DRIHack.end *",
                "loader.<init> *",
                "unpackclass.<init> *"
            ),
            maxObfuscatedNameLen = 2
        )
    }
}
