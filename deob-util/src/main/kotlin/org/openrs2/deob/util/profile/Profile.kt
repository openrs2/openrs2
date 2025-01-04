package org.openrs2.deob.util.profile

import org.openrs2.asm.filter.GlobClassFilter
import org.openrs2.asm.filter.GlobMemberFilter

public class Profile(
    public val excludedClasses: GlobClassFilter,
    public val excludedMethods: GlobMemberFilter,
    public val excludedFields: GlobMemberFilter,
    public val entryPoints: GlobMemberFilter,
    public val maxObfuscatedNameLen: Int,
    public val transformers: List<String>,
    public val scrambledLibraries: Set<String> = emptySet()
)
