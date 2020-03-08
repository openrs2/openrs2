package dev.openrs2.deob.ir.translation

import com.google.common.graph.GraphBuilder
import dev.openrs2.deob.analysis.ControlFlowAnalyzer
import dev.openrs2.deob.ir.Method
import dev.openrs2.deob.ir.flow.BasicBlock
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import java.util.Stack

class IrDecompiler(private val owner: ClassNode, private val method: MethodNode) :
    Analyzer<BasicValue>(BasicInterpreter()) {

    private val graph = GraphBuilder
        .directed()
        .allowsSelfLoops(true)
        .build<BasicBlock>()

    private val blocks = mutableMapOf<Int, BasicBlock>()
    private fun bb(index: Int) = blocks.computeIfAbsent(index) {
        val bb = BasicBlock(graph, method.instructions, it, it)
        val leader = method.instructions[index]

        if (leader is LabelNode) {
            bb.label = leader
        }

        bb
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        graph.putEdge(bb(insnIndex), bb(successorIndex))
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        // @TODO: Attach basic blocks with exception types.
        return true
    }

    fun decompile(): Method {
        analyze(owner.name, method)

        val entryBlock = blocks[0] ?: throw IllegalStateException("No method entry block found")

        val remainingBlocks = Stack<BasicBlock>()
        remainingBlocks.push(entryBlock)

        val visited = mutableSetOf<BasicBlock>()

        while (remainingBlocks.isNotEmpty()) {
            val bb = remainingBlocks.pop()
            var next = bb.findTrivialSuccessor()

            while (next != null && next != bb) {
                bb.merge(next)

                next = bb.findTrivialSuccessor()
            }

            for (succ in bb.successors()) {
                if (!visited.contains(succ)) {
                    remainingBlocks.push(succ)
                }
            }

            visited.add(bb)
        }

        return Method(owner, method, entryBlock)
    }
}

private fun BasicBlock.findTrivialSuccessor(): BasicBlock? {
    val successors = successors()
    if (successors.size != 1) return null

    val successor = successors.first()!!
    val nextPredecessors = successor.predecessors()

    if (nextPredecessors.size == 1) {
        return successor
    } else {
        return null
    }
}
