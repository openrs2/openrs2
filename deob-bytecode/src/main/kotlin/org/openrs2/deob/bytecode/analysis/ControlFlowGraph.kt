package org.openrs2.deob.bytecode.analysis

import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.EdgeReversedGraph

public class ControlFlowGraph(
    public val entryNodes: Set<Int>,
    public val exitNodes: Set<Int>,
    private val graph: Graph<Int, DefaultEdge>
) : Graph<Int, DefaultEdge> by graph {
    public fun reverse(): ControlFlowGraph {
        return ControlFlowGraph(exitNodes, entryNodes, EdgeReversedGraph(graph))
    }
}
