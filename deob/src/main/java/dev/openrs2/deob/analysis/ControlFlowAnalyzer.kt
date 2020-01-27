package dev.openrs2.deob.analysis

import com.google.common.graph.Graph
import com.google.common.graph.GraphBuilder
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

class ControlFlowAnalyzer : Analyzer<BasicValue>(BasicInterpreter()) {
    private val graph = GraphBuilder
        .directed()
        .allowsSelfLoops(true)
        .immutable<Int>()

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        graph.putEdge(insnIndex, successorIndex)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        graph.putEdge(insnIndex, successorIndex)
        return true
    }

    fun createGraph(owner: String, method: MethodNode): Graph<Int> {
        analyze(owner, method)
        return graph.build()
    }
}
