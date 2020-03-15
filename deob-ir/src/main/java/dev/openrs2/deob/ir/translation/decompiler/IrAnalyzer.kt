package dev.openrs2.deob.ir.translation.decompiler

import com.google.common.graph.ValueGraphBuilder
import dev.openrs2.deob.ir.AssignmentStmt
import dev.openrs2.deob.ir.BasicBlock
import dev.openrs2.deob.ir.BasicBlockGraph
import dev.openrs2.deob.ir.BinaryExpr
import dev.openrs2.deob.ir.CallExpr
import dev.openrs2.deob.ir.CallStmt
import dev.openrs2.deob.ir.Expr
import dev.openrs2.deob.ir.IfStmt
import dev.openrs2.deob.ir.ReturnStmt
import dev.openrs2.deob.ir.StmtVisitor
import dev.openrs2.deob.ir.VarExpr
import dev.openrs2.deob.ir.flow.ControlFlowTransfer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Analyzer

class IrAnalyzer(private val interpreter: IrInterpreter) : StmtVisitor, Analyzer<IrValue>(interpreter) {
    init {
        interpreter.visitor = this
    }

    /**
     * A mapping of program counters (instruction offsets) to the [BasicBlock]s
     * that define them.
     */
    private lateinit var blocks: MutableMap<Int, BasicBlock>

    /**
     * The control flow graph defining the method.
     */
    private lateinit var graph: BasicBlockGraph

    /**
     * The current [BasicBlock] of the method that is being analyzed.
     */
    private lateinit var bb: BasicBlock

    /**
     * The code belonging to the current method being analyzed.
     */
    private lateinit var code: InsnList

    /**
     * Get or create a new basic block that begins at the instruction with the given [index].
     */
    private fun bb(index: Int) = blocks.computeIfAbsent(index) { BasicBlock(graph) }

    /**
     * Run the analyzer on the [method] owned by the class with name [owner] and return the [BasicBlock]
     * that defines method entry.
     */
    fun decode(owner: String, method: MethodNode): BasicBlock {
        analyze(owner, method)
        return blocks[0] ?: error("No entry BasicBlock found")
    }

    override fun init(owner: String, method: MethodNode) {
        graph = ValueGraphBuilder.directed()
            .allowsSelfLoops(true)
            .build()

        bb = BasicBlock(graph)
        code = method.instructions
        blocks = mutableMapOf(0 to bb)
    }

    override fun newControlFlowEdge(insnIndex: Int, successorIndex: Int) {
        when (val insn = code[insnIndex]) {
            is JumpInsnNode -> {
                val transfer = if (insn.opcode == Opcodes.GOTO) {
                    ControlFlowTransfer.ConditionalJump(code[successorIndex] == insn.label)
                } else {
                    ControlFlowTransfer.Goto
                }

                graph.putEdgeValue(bb, bb(successorIndex), transfer)
            }
        }

        val startsBlock = blocks[insnIndex]
        if (startsBlock != null) {
            bb = startsBlock
        }
    }

    override fun visitAssignmen(variable: VarExpr, value: Expr) {
        bb.statements.add(AssignmentStmt(variable, value))
    }

    override fun visitCall(expr: CallExpr) {
        bb.statements.add(CallStmt(expr))
    }

    override fun visitIf(conditional: BinaryExpr) {
        bb.statements.add(IfStmt(conditional))
    }

    override fun visitReturn(value: Expr?) {
        bb.statements.add(ReturnStmt(value))
    }
}
