package dev.openrs2.deob.cli.ir

import com.google.common.graph.EndpointPair
import dev.openrs2.deob.ir.Method
import dev.openrs2.deob.ir.flow.BasicBlock
import org.jgrapht.Graph
import org.jgrapht.graph.guava.MutableGraphAdapter
import org.jgrapht.nio.DefaultAttribute
import org.jgrapht.nio.GraphExporter
import org.jgrapht.nio.dot.DOTExporter

typealias BlockGraph = Graph<BasicBlock, EndpointPair<BasicBlock>>
typealias BlockGraphExporter = GraphExporter<BasicBlock, EndpointPair<BasicBlock>>

fun dotExporter(): BlockGraphExporter {
    val exporter = DOTExporter<BasicBlock, EndpointPair<BasicBlock>>()

    exporter.setVertexAttributeProvider {
        val label = it.toString().replace("\n", "\\l")

        mapOf(
            "label" to DefaultAttribute.createAttribute(label)
        )
    }

    return exporter
}

object PrintCfgCommand : MethodScopedCommand("ir-print-cfg") {
    override fun run(method: Method) {
        val graph: BlockGraph = MutableGraphAdapter(method.cfg)
        val exporter: BlockGraphExporter = dotExporter()

        exporter.exportGraph(graph, System.out)
    }
}
