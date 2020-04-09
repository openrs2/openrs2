package dev.openrs2.deob.analysis

import dev.openrs2.util.collect.removeFirstOrNull
import org.jgrapht.Graph
import org.jgrapht.graph.DefaultEdge
import org.jgrapht.graph.EdgeReversedGraph
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode

abstract class DataFlowAnalyzer<T>(owner: String, private val method: MethodNode, backwards: Boolean = false) {
    private val graph: Graph<Int, DefaultEdge>
    private val inSets = mutableMapOf<Int, T>()
    private val outSets = mutableMapOf<Int, T>()

    init {
        val forwardsGraph = ControlFlowAnalyzer().createGraph(owner, method)
        graph = if (backwards) {
            EdgeReversedGraph(forwardsGraph)
        } else {
            forwardsGraph
        }
    }

    protected abstract fun createInitialSet(): T
    protected abstract fun join(set1: T, set2: T): T
    protected abstract fun transfer(set: T, insn: AbstractInsnNode): T

    fun getInSet(insn: AbstractInsnNode): T? {
        return getInSet(method.instructions.indexOf(insn))
    }

    fun getInSet(index: Int): T? {
        return inSets[index]
    }

    fun getOutSet(insn: AbstractInsnNode): T? {
        return getOutSet(method.instructions.indexOf(insn))
    }

    fun getOutSet(index: Int): T? {
        return outSets[index]
    }

    fun analyze() {
        for (node in graph.vertexSet()) {
            outSets[node] = createInitialSet()
        }

        val workList = LinkedHashSet<Int>(graph.vertexSet())
        while (true) {
            val node = workList.removeFirstOrNull() ?: break

            val predecessors = graph.incomingEdgesOf(node).map { edge -> outSets[graph.getEdgeSource(edge)]!! }

            val inSet = if (predecessors.isEmpty()) {
                createInitialSet()
            } else {
                predecessors.reduce(this::join)
            }

            inSets[node] = inSet

            val outSet = transfer(inSet, method.instructions[node])

            if (outSets[node] != outSet) {
                outSets[node] = outSet

                for (edge in graph.outgoingEdgesOf(node)) {
                    val successor = graph.getEdgeTarget(edge)
                    workList += successor
                }
            }
        }
    }
}
