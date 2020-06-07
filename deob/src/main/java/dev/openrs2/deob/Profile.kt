package dev.openrs2.deob

import dev.openrs2.asm.filter.GlobClassFilter
import dev.openrs2.asm.filter.GlobMemberFilter

class Profile(
    val excludedClasses: GlobClassFilter,
    val excludedMethods: GlobMemberFilter,
    val excludedFields: GlobMemberFilter,
    val entryPoints: GlobMemberFilter,
    val scrambledLibraries: Set<String>,
    val maxObfuscatedNameLen: Int
)
