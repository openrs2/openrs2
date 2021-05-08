package org.openrs2.patcher

public enum class Architecture(
    public val needles: List<String>
) {
    I386(listOf("i386", "x86")),
    AMD64(listOf("amd64", "x86_64")),
    AARCH64(listOf("aarch64"));

    public val id: String = name.lowercase()
}
