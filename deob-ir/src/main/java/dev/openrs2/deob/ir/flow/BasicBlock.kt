package dev.openrs2.deob.ir.flow

import com.google.common.graph.MutableGraph
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor

class BasicBlock(
    internal val graph: MutableGraph<BasicBlock>,
    private val code: InsnList,
    private var start: Int,
    private var end: Int
) {
    fun successors() : Set<BasicBlock> = graph.successors(this)
    fun predecessors() : Set<BasicBlock> = graph.predecessors(this)

    var label: LabelNode? = null

    fun merge(other: BasicBlock) {
        for (succ in other.successors()) {
            graph.putEdge(this, succ)
        }

        graph.removeEdge(this, other)
        graph.removeNode(other)

        end = other.end
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return System.identityHashCode(this)
    }

    override fun toString(): String {
        val textifier = Textifier()
        val methodTracer = TraceMethodVisitor(textifier)

        code.accept(methodTracer)

        val text = textifier.text.subList(start, end + 1) as List<String>
        return text.reduce { l, r -> l + r }
    }
}
