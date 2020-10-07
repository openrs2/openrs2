package org.openrs2.deob.analysis

import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value

public data class IntValue(val basicValue: BasicValue, val set: IntValueSet = IntValueSet.Unknown) : Value {
    override fun getSize(): Int {
        return basicValue.size
    }
}
