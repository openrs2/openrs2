package org.openrs2.patcher

import org.openrs2.patcher.Architecture.AARCH64
import org.openrs2.patcher.Architecture.AMD64
import org.openrs2.patcher.Architecture.I386

public enum class OperatingSystem(
    public val needle: String,
    public val architectures: List<Architecture>,
    public val glLibraries: List<String>
) {
    // AMD64 must be before I386 as x86 is a substring of x86_64
    WINDOWS("win", listOf(AMD64, I386), listOf("jaggl.dll")),
    MAC("mac", listOf(AARCH64, AMD64, I386), listOf("libjaggl.dylib")),
    LINUX("linux", listOf(AMD64, I386), listOf("libjaggl.so", "libjaggl_dri.so"));

    public val id: String = name.lowercase()
}
