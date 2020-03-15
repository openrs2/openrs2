package dev.openrs2.deob.ir.flow.data

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class LiveVariableAnalyzer(owner: String, method: MethodNode) :
    DataFlowAnalyzer<Set<Int>>(owner, method, backwards = true) {
    override fun createInitialSet(): Set<Int> {
        return emptySet()
    }

    override fun join(set1: Set<Int>, set2: Set<Int>): Set<Int> {
        return set1 union set2
    }

    override fun transfer(set: Set<Int>, insn: AbstractInsnNode): Set<Int> {
        return when (insn) {
            is VarInsnNode -> when (insn.opcode) {
                Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> set.plus(insn.`var`)
                Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> set.minus(insn.`var`)
                else -> set
            }
            is IincInsnNode -> set.plus(insn.`var`)
            else -> set
        }
    }
}
