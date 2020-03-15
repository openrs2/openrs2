package dev.openrs2.deob.ir

import com.google.common.graph.MutableValueGraph
import dev.openrs2.deob.ir.flow.ControlFlowTransfer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class Method(val owner: ClassNode, val method: MethodNode, val entry: BasicBlock) {
    val cfg: MutableValueGraph<BasicBlock, ControlFlowTransfer>
        get() = entry.graph
}
