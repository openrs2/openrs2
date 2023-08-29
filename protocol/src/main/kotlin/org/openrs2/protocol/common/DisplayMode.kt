package org.openrs2.protocol.common

public enum class DisplayMode {
    SD,
    HD_SMALL,
    HD,
    HD_FULLSCREEN;

    public companion object {
        private val values = values()

        public fun fromOrdinal(ordinal: Int): DisplayMode? {
            return if (ordinal >= 0 && ordinal < values.size) {
                values[ordinal]
            } else {
                null
            }
        }
    }
}
