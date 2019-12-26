package dev.openrs2.deob.analysis

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value

sealed class ConstSourceValue : Value {
    data class Unknown(override val basicValue: BasicValue) : ConstSourceValue()
    data class Single(override val basicValue: BasicValue, val source: AbstractInsnNode) : ConstSourceValue()

    abstract val basicValue: BasicValue

    override fun getSize(): Int {
        return basicValue.size
    }
}
