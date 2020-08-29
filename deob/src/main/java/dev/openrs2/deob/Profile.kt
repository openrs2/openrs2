package dev.openrs2.deob

import dev.openrs2.asm.filter.GlobClassFilter
import dev.openrs2.asm.filter.GlobMemberFilter

public class Profile(
    public val excludedClasses: GlobClassFilter,
    public val excludedMethods: GlobMemberFilter,
    public val excludedFields: GlobMemberFilter,
    public val entryPoints: GlobMemberFilter,
    public val scrambledLibraries: Set<String>,
    public val maxObfuscatedNameLen: Int
)
