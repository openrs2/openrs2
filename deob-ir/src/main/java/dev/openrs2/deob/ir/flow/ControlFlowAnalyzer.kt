package dev.openrs2.deob.ir.flow

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue
import java.util.Stack

/**
 * A bytecode analyzer that produces a graph representing the basic control structure of a method.
 *
 * @param reduce if the graph should be reduced by coalescing all nodes with an immediate dominator
 * with only a single successor.
 */
class ControlFlowAnalyzer(private val reduce: Boolean = false) : Analyzer<BasicValue>(BasicInterpreter()) {
    val instructionsToBlocks = mutableMapOf<Int, Int>()

    private val graph = GraphBuilder
        .directed()
        .allowsSelfLoops(true)
        .build<Int>()

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        graph.putEdge(insnIndex, successorIndex)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        graph.putEdge(insnIndex, successorIndex)
        return true
    }

    private fun reduceGraph() {
        val nodeQueue = Stack<Int>()
        val nodesVisited = mutableSetOf<Int>()

        nodeQueue.push(0)

        while (nodeQueue.isNotEmpty()) {
            // Coalesce all nodes that are immediately dominated and are the sole successor
            // of their dominator
            val current = nodeQueue.pop()
            val nextSuccessors = generateSequence { graph.successors(current).singleOrNull() }.iterator()

            for (successor in nextSuccessors) {
                val isImmediateDominator = current == graph.predecessors(successor).singleOrNull()

                if (!isImmediateDominator || successor == current) {
                    break
                }

                for (domSuccessor in graph.successors(successor)) {
                    graph.putEdge(current, domSuccessor)
                }

                graph.removeEdge(current, successor)
                graph.removeNode(successor)
            }

            for (successor in graph.successors(current)) {
                if (!nodesVisited.contains(successor)) {
                    nodeQueue.push(successor)
                }
            }

            nodesVisited += current
        }
    }

    fun createGraph(owner: String, method: MethodNode): Graph<Int> {
        analyze(owner, method)

        if (reduce) {
            reduceGraph()
        }

        return graph
    }
}
