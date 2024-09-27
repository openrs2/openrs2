package org.openrs2.protocol.common

public enum class AntiAliasingMode {
    NONE,
    X2,
    X4;

    public companion object {
        public fun fromOrdinal(ordinal: Int): AntiAliasingMode? {
            return if (ordinal >= 0 && ordinal < entries.size) {
                entries[ordinal]
            } else {
                null
            }
        }
    }
}
