package org.openrs2.deob.bytecode.analysis

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import java.util.Collections

public class CopyPropagationAnalyzer(owner: String, method: MethodNode) :
    DataFlowAnalyzer<Set<CopyAssignment>>(owner, method) {

    private val allAssignments = mutableSetOf<CopyAssignment>()

    init {
        for (insn in method.instructions) {
            if (insn !is VarInsnNode || !STORE_OPCODES.contains(insn.opcode)) {
                continue
            }

            val previous = insn.previous
            if (previous !is VarInsnNode || !LOAD_OPCODES.contains(previous.opcode)) {
                continue
            }

            allAssignments += CopyAssignment(insn.`var`, previous.`var`)
        }
    }

    override fun createEntrySet(): Set<CopyAssignment> {
        return Collections.emptySet()
    }

    override fun createInitialSet(): Set<CopyAssignment> {
        return allAssignments
    }

    override fun join(set1: Set<CopyAssignment>, set2: Set<CopyAssignment>): Set<CopyAssignment> {
        return set1 intersect set2
    }

    override fun transfer(set: Set<CopyAssignment>, insn: AbstractInsnNode): Set<CopyAssignment> {
        return when {
            insn is VarInsnNode && STORE_OPCODES.contains(insn.opcode) -> {
                val newSet = set.minusKilledByAssignmentTo(insn.`var`)

                val previous = insn.previous
                if (previous is VarInsnNode && LOAD_OPCODES.contains(previous.opcode)) {
                    newSet.plus(CopyAssignment(insn.`var`, previous.`var`))
                } else {
                    newSet
                }
            }

            insn is IincInsnNode -> set.minusKilledByAssignmentTo(insn.`var`)
            else -> set
        }
    }

    private fun Set<CopyAssignment>.minusKilledByAssignmentTo(index: Int): Set<CopyAssignment> {
        return filterTo(mutableSetOf()) { it.source != index && it.destination != index }
    }

    private companion object {
        private val LOAD_OPCODES = setOf(
            Opcodes.ILOAD,
            Opcodes.LSTORE,
            Opcodes.FLOAD,
            Opcodes.DLOAD,
            Opcodes.ALOAD
        )
        private val STORE_OPCODES = setOf(
            Opcodes.ISTORE,
            Opcodes.LSTORE,
            Opcodes.FSTORE,
            Opcodes.DSTORE,
            Opcodes.ASTORE
        )
    }
}
