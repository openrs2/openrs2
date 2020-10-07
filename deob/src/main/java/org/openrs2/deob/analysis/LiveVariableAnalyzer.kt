package org.openrs2.deob.analysis

import it.unimi.dsi.fastutil.ints.IntOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import it.unimi.dsi.fastutil.ints.IntSets
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

public class LiveVariableAnalyzer(owner: String, method: MethodNode) :
    DataFlowAnalyzer<IntSet>(owner, method, backwards = true) {

    override fun createInitialSet(): IntSet {
        return IntSets.EMPTY_SET
    }

    override fun join(set1: IntSet, set2: IntSet): IntSet {
        return set1 union set2
    }

    override fun transfer(set: IntSet, insn: AbstractInsnNode): IntSet {
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

    private infix fun IntSet.union(other: IntSet): IntSet {
        if (this == other || other.isEmpty()) {
            return this
        } else if (isEmpty()) {
            return other
        }

        val set = IntOpenHashSet(this)
        set.addAll(other)
        return set
    }

    private fun IntSet.plus(element: Int): IntSet {
        if (contains(element)) {
            return this
        } else if (isEmpty()) {
            return IntSets.singleton(element)
        }

        val newSet = IntOpenHashSet(this)
        newSet.add(element)
        return newSet
    }

    private fun IntSet.minus(element: Int): IntSet {
        if (!contains(element)) {
            return this
        } else if (size == 1) {
            return IntSets.EMPTY_SET
        }

        val newSet = IntOpenHashSet(this)
        newSet.remove(element)
        return newSet
    }
}
