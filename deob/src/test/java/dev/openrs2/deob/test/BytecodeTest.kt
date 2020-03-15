package dev.openrs2.deob.test

import org.objectweb.asm.Opcodes
import org.objectweb.asm.commons.InstructionAdapter
import org.objectweb.asm.tree.MethodNode

@DslMarker
annotation class BytecodeDslMarker

enum class Expectation {
    Removed,
    Added,
    Present
}

data class BytecodeTest(val code: MethodNode, val expectations: List<Expectation>)

@BytecodeDslMarker
class BytecodeTestBuilder : InstructionAdapter(Opcodes.ASM7, MethodNode()) {
    val method = mv as MethodNode
    val expectations = mutableListOf<Expectation>()

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
        expectations.add(Expectation.Present)
    }

    operator fun Unit.unaryMinus() {
        expectations[expectations.size - 1] = Expectation.Removed
    }

    operator fun Unit.unaryPlus() {
        expectations[expectations.size - 1] = Expectation.Added
    }
}
