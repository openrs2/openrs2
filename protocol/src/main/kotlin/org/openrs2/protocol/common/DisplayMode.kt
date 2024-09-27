package org.openrs2.protocol.common

public enum class DisplayMode {
    SD,
    HD_SMALL,
    HD,
    HD_FULLSCREEN;

    public companion object {
        public fun fromOrdinal(ordinal: Int): DisplayMode? {
            return if (ordinal >= 0 && ordinal < entries.size) {
                entries[ordinal]
            } else {
                null
            }
        }
    }
}
