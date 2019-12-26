package dev.openrs2.deob.analysis

import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value

sealed class IntValue : Value {
    data class Unknown(override val basicValue: BasicValue) : IntValue()
    data class Constant(override val basicValue: BasicValue, val values: Set<Int>) : IntValue() {
        val singleton: Int?

        init {
            require(values.isNotEmpty())

            singleton = if (values.size == 1) values.first() else null
        }

        constructor(basicValue: BasicValue, value: Int) : this(basicValue, setOf(value))
    }

    abstract val basicValue: BasicValue

    override fun getSize(): Int {
        return basicValue.size
    }
}
