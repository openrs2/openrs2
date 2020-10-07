package org.openrs2.deob.analysis

import org.objectweb.asm.Opcodes.IFEQ
import org.objectweb.asm.Opcodes.IFGE
import org.objectweb.asm.Opcodes.IFGT
import org.objectweb.asm.Opcodes.IFLE
import org.objectweb.asm.Opcodes.IFLT
import org.objectweb.asm.Opcodes.IFNE
import org.objectweb.asm.Opcodes.IF_ICMPEQ
import org.objectweb.asm.Opcodes.IF_ICMPGE
import org.objectweb.asm.Opcodes.IF_ICMPGT
import org.objectweb.asm.Opcodes.IF_ICMPLE
import org.objectweb.asm.Opcodes.IF_ICMPLT
import org.objectweb.asm.Opcodes.IF_ICMPNE

public object IntBranch {
    public fun evaluateUnary(opcode: Int, values: Set<Int>): IntBranchResult {
        require(values.isNotEmpty())

        var taken = 0
        var notTaken = 0

        for (v in values) {
            if (evaluateUnary(opcode, v)) {
                taken++
            } else {
                notTaken++
            }
        }

        return IntBranchResult.fromTakenNotTaken(taken, notTaken)
    }

    private fun evaluateUnary(opcode: Int, value: Int): Boolean {
        return when (opcode) {
            IFEQ -> value == 0
            IFNE -> value != 0
            IFLT -> value < 0
            IFGE -> value >= 0
            IFGT -> value > 0
            IFLE -> value <= 0
            else -> throw IllegalArgumentException("Opcode $opcode is not a unary conditional branch instruction")
        }
    }

    public fun evaluateBinary(opcode: Int, values1: Set<Int>, values2: Set<Int>): IntBranchResult {
        require(values1.isNotEmpty())
        require(values2.isNotEmpty())

        var taken = 0
        var notTaken = 0

        for (v1 in values1) {
            for (v2 in values2) {
                if (evaluateBinary(opcode, v1, v2)) {
                    taken++
                } else {
                    notTaken++
                }
            }
        }

        return IntBranchResult.fromTakenNotTaken(taken, notTaken)
    }

    private fun evaluateBinary(opcode: Int, value1: Int, value2: Int): Boolean {
        return when (opcode) {
            IF_ICMPEQ -> value1 == value2
            IF_ICMPNE -> value1 != value2
            IF_ICMPLT -> value1 < value2
            IF_ICMPGE -> value1 >= value2
            IF_ICMPGT -> value1 > value2
            IF_ICMPLE -> value1 <= value2
            else -> throw IllegalArgumentException("Opcode $opcode is not a binary conditional branch instruction")
        }
    }
}
