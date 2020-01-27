package dev.openrs2.deob.analysis

import com.google.common.graph.Graph
import com.google.common.graph.Graphs
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.MethodNode

abstract class DataFlowAnalyzer<T>(owner: String, private val method: MethodNode, backwards: Boolean = false) {
    private val graph: Graph<Int>
    private val inSets = mutableMapOf<Int, T>()
    private val outSets = mutableMapOf<Int, T>()

    init {
        val forwardsGraph = ControlFlowAnalyzer().createGraph(owner, method)
        graph = if (backwards) {
            Graphs.transpose(forwardsGraph)
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
        for (node in graph.nodes()) {
            outSets[node] = createInitialSet()
        }

        var changed: Boolean
        do {
            changed = false

            for (node in graph.nodes()) {
                val predecessors = graph.predecessors(node).map { pred -> outSets[pred]!! }

                val inSet = if (predecessors.isEmpty()) {
                    createInitialSet()
                } else {
                    predecessors.reduce(this::join)
                }

                inSets[node] = inSet

                val outSet = transfer(inSet, method.instructions[node])

                if (outSets[node] != outSet) {
                    outSets[node] = outSet
                    changed = true
                }
            }
        } while (changed)
    }
}
