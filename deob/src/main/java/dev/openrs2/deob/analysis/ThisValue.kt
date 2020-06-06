package dev.openrs2.deob.analysis

import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value

data class ThisValue(val basicValue: BasicValue, val isThis: Boolean) : Value {
    override fun getSize(): Int {
        return basicValue.size
    }
}
