package org.openrs2.deob.bytecode.analysis

public sealed class IntValueSet {
    public data class Constant(val values: Set<Int>) : IntValueSet() {
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

    public object Unknown : IntValueSet() {
        override val singleton: Int?
            get() = null

        override fun union(other: IntValueSet): IntValueSet {
            return Unknown
        }
    }

    public abstract val singleton: Int?
    public abstract infix fun union(other: IntValueSet): IntValueSet

    public companion object {
        public fun singleton(value: Int): IntValueSet {
            return Constant(setOf(value))
        }
    }
}
