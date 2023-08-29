package org.openrs2.protocol.common

public enum class AntiAliasingMode {
    NONE,
    X2,
    X4;

    public companion object {
        private val values = values()

        public fun fromOrdinal(ordinal: Int): AntiAliasingMode? {
            return if (ordinal >= 0 && ordinal < values.size) {
                values[ordinal]
            } else {
                null
            }
        }
    }
}
