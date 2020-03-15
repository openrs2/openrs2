package dev.openrs2.deob.ir

import com.google.common.graph.MutableValueGraph
import dev.openrs2.deob.ir.flow.ControlFlowTransfer

typealias BasicBlockGraph = MutableValueGraph<BasicBlock, ControlFlowTransfer>

class BasicBlock(
    internal val graph: BasicBlockGraph
) {
    val statements = mutableListOf<Stmt>()

    fun successors(): Set<BasicBlock> = graph.successors(this)
    fun predecessors(): Set<BasicBlock> = graph.predecessors(this)

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun toString(): String {
        return "BasicBlock(expressions=\n${statements.joinToString("\n")}\n)"
    }
}
