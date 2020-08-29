package dev.openrs2.deob.analysis

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicValue
import org.objectweb.asm.tree.analysis.Value

public sealed class ConstSourceValue : Value {
    public data class Unknown(override val basicValue: BasicValue) : ConstSourceValue()
    public data class Insn(override val basicValue: BasicValue, val source: AbstractInsnNode) : ConstSourceValue()
    public data class Arg(override val basicValue: BasicValue) : ConstSourceValue()

    public abstract val basicValue: BasicValue

    override fun getSize(): Int {
        return basicValue.size
    }
}
