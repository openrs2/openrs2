package org.openrs2.deob.bytecode.analysis

import org.jgrapht.graph.AsUnmodifiableGraph
import org.jgrapht.graph.DefaultDirectedGraph
import org.jgrapht.graph.DefaultEdge
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

public class ControlFlowAnalyzer : Analyzer<BasicValue>(BasicInterpreter()) {
    private val graph = DefaultDirectedGraph<Int, DefaultEdge>(DefaultEdge::class.java)

    init {
        graph.addVertex(0)
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        graph.addVertex(insnIndex)
        graph.addVertex(successorIndex)
        graph.addEdge(insnIndex, successorIndex)
    }

    override fun newControlFlowExceptionEdge(insnIndex: Int, successorIndex: Int): Boolean {
        newControlFlowEdge(insnIndex, successorIndex)
        return true
    }

    public fun createGraph(owner: String, method: MethodNode): ControlFlowGraph {
        analyze(owner, method)

        val entryNodes = setOf(0)
        val exitNodes = buildSet {
            for ((i, insn) in method.instructions.withIndex()) {
                if (graph.containsVertex(i) && insn.opcode in EXIT_NODE_OPCODES) {
                    add(i)
                }
            }
        }

        return ControlFlowGraph(entryNodes, exitNodes, AsUnmodifiableGraph(graph))
    }

    private companion object {
        private val EXIT_NODE_OPCODES = setOf(
            RETURN,
            IRETURN,
            LRETURN,
            FRETURN,
            DRETURN,
            ARETURN,
            ATHROW,
        )
    }
}
