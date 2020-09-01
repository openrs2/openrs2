package dev.openrs2.cache

public enum class Js5Protocol {
    ORIGINAL,
    VERSIONED,
    SMART;

    public val id: Int
        get() = ordinal + OFFSET

    public companion object {
        private const val OFFSET = 5

        public fun fromId(id: Int): Js5Protocol? {
            val ordinal = id - OFFSET
            val values = values()
            return if (ordinal >= 0 && ordinal < values.size) {
                values[ordinal]
            } else {
                null
            }
        }
    }
}
