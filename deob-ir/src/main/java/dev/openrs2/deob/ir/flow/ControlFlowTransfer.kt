package dev.openrs2.deob.ir.flow

sealed class ControlFlowTransfer {

    data class ConditionalJump(val successful: Boolean) : ControlFlowTransfer()

    object Goto : ControlFlowTransfer()
}
