package org.openrs2.deob.util.profile

import org.openrs2.asm.filter.GlobClassFilter
import org.openrs2.asm.filter.GlobMemberFilter
import org.openrs2.deob.util.module.ModuleSpec
import java.nio.file.Path

public class Profile(
    public val directory: Path,
    public val mapping: Path,
    public val modules: List<ModuleSpec>,
    public val preprocessors: List<String> = emptyList(),
    public val excludedClasses: GlobClassFilter,
    public val excludedMethods: GlobMemberFilter,
    public val excludedFields: GlobMemberFilter,
    public val entryPoints: GlobMemberFilter,
    public val maxObfuscatedNameLen: Int,
    public val transformers: List<String>,
    public val scrambledLibraries: Set<String> = emptySet()
)
