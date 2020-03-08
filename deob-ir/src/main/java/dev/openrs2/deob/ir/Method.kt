package dev.openrs2.deob.ir

import com.google.common.graph.MutableGraph
import dev.openrs2.deob.ir.flow.BasicBlock
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class Method(val owner: ClassNode, val method: MethodNode, val entry: BasicBlock) {
    val cfg: MutableGraph<BasicBlock>
        get() = entry.graph
}
