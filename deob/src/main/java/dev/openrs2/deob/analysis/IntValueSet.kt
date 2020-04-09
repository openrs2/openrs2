package dev.openrs2.deob.analysis

sealed class IntValueSet {
    data class Constant(val values: Set<Int>) : IntValueSet() {
        init {
            require(values.isNotEmpty())
        }

        override val singleton: Int?
            get() = if (values.size == 1) {
                values.first()
            } else {
                null
            }

        override fun union(other: IntValueSet): IntValueSet {
            return if (other is Constant) {
                Constant(values union other.values)
            } else {
                Unknown
            }
        }
    }

    object Unknown : IntValueSet() {
        override val singleton: Int?
            get() = null

        override fun union(other: IntValueSet): IntValueSet {
            return Unknown
        }
    }

    abstract val singleton: Int?
    abstract infix fun union(other: IntValueSet): IntValueSet

    companion object {
        fun singleton(value: Int): IntValueSet {
            return Constant(setOf(value))
        }
    }
}
