package dev.openrs2.deob.analysis

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Interpreter

public class ThisInterpreter : Interpreter<ThisValue>(Opcodes.ASM8) {
    private val basicInterpreter = BasicInterpreter()

    override fun newValue(type: Type?): ThisValue? {
        val basicValue = basicInterpreter.newValue(type) ?: return null
        return ThisValue(basicValue, false)
    }

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): ThisValue {
        val basicValue = basicInterpreter.newParameterValue(isInstanceMethod, local, type)
        return ThisValue(basicValue, isInstanceMethod && local == 0)
    }

    override fun newOperation(insn: AbstractInsnNode): ThisValue {
        val basicValue = basicInterpreter.newOperation(insn)
        return ThisValue(basicValue, false)
    }

    override fun copyOperation(insn: AbstractInsnNode, value: ThisValue): ThisValue {
        val basicValue = basicInterpreter.copyOperation(insn, value.basicValue)
        /*
         * Only allow "this"ness to propagate from a variable to the stack, not
         * vice-versa. This is compatible with javac's analysis.
         */
        return if (insn.opcode == Opcodes.ASTORE) {
            ThisValue(basicValue, false)
        } else {
            ThisValue(basicValue, value.isThis)
        }
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: ThisValue): ThisValue? {
        val basicValue = basicInterpreter.unaryOperation(insn, value.basicValue) ?: return null
        return ThisValue(basicValue, false)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: ThisValue, value2: ThisValue): ThisValue? {
        val basicValue = basicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue)
            ?: return null
        return ThisValue(basicValue, false)
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: ThisValue,
        value2: ThisValue,
        value3: ThisValue
    ): ThisValue? {
        val basicValue = basicInterpreter.ternaryOperation(
            insn,
            value1.basicValue,
            value2.basicValue,
            value3.basicValue
        ) ?: return null
        return ThisValue(basicValue, false)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<ThisValue>): ThisValue? {
        val args = values.map(ThisValue::basicValue)
        val basicValue = basicInterpreter.naryOperation(insn, args) ?: return null
        return ThisValue(basicValue, false)
    }

    override fun returnOperation(insn: AbstractInsnNode, value: ThisValue, expected: ThisValue) {
        basicInterpreter.returnOperation(insn, value.basicValue, expected.basicValue)
    }

    override fun merge(value1: ThisValue, value2: ThisValue): ThisValue {
        val basicValue = basicInterpreter.merge(value1.basicValue, value2.basicValue)
        return ThisValue(basicValue, value1.isThis && value2.isThis)
    }
}
