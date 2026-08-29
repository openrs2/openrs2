package org.openrs2.deob.util.module

public enum class ModuleType(public val synthetic: Boolean = false) {
    CLIENT,
    GL,
    LOADER,
    SIGNLINK(synthetic = true),
    UNPACK(synthetic = true),
    UNPACKCLASS;

    public val library: String
        get() = name.lowercase()
}
