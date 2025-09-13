package org.openrs2.deob.bytecode

import org.openrs2.asm.filter.GlobClassFilter
import org.openrs2.asm.filter.GlobMemberFilter

public class Profile(
    public val libraries: Map<String, LibraryProfile>,
    public val dependencies: Map<String, DependencyProfile>?,
    public val excludedClasses: GlobClassFilter,
    public val excludedMethods: GlobMemberFilter,
    public val excludedFields: GlobMemberFilter,
    public val entryPoints: GlobMemberFilter,
    public val scrambledLibraries: Set<String>,
    public val maxObfuscatedNameLen: Int,
    public val transformers: List<String>
) {
    public class LibraryProfile(
        public val format: String?,
        public val file: String?,
        public val requires: Set<String>?,
        public val signedMove: Set<String>?,
        public val move: Set<String>?
    )

    public class DependencyProfile(
        public val format: String,
        public val file: String
    )
}
