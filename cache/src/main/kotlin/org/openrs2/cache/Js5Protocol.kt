package org.openrs2.cache

public enum class Js5Protocol {
    ORIGINAL,
    VERSIONED,
    SMART;

    public val id: Int
        get() = ordinal + OFFSET

    public companion object {
        private const val OFFSET = 5

        @JvmStatic
        public fun fromId(id: Int): Js5Protocol? {
            val ordinal = id - OFFSET
            return if (ordinal >= 0 && ordinal < entries.size) {
                entries[ordinal]
            } else {
                null
            }
        }
    }
}
