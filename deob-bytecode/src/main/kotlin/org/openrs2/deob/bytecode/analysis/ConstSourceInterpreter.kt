package org.openrs2.deob.bytecode.analysis

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Interpreter
import org.openrs2.asm.intConstant

public class ConstSourceInterpreter : Interpreter<ConstSourceValue>(Opcodes.ASM9) {
    private val basicInterpreter = BasicInterpreter()

    override fun newValue(type: Type?): ConstSourceValue? {
        val basicValue = basicInterpreter.newValue(type) ?: return null
        return ConstSourceValue.Unknown(basicValue)
    }

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): ConstSourceValue {
        val basicValue = basicInterpreter.newParameterValue(isInstanceMethod, local, type)
        return ConstSourceValue.Arg(basicValue)
    }

    override fun newOperation(insn: AbstractInsnNode): ConstSourceValue {
        val basicValue = basicInterpreter.newOperation(insn)
        return if (insn.intConstant != null) {
            ConstSourceValue.Insn(basicValue, insn)
        } else {
            ConstSourceValue.Unknown(basicValue)
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: ConstSourceValue): ConstSourceValue {
        val basicValue = basicInterpreter.copyOperation(insn, value.basicValue)
        return ConstSourceValue.Unknown(basicValue)
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: ConstSourceValue): ConstSourceValue? {
        val basicValue = basicInterpreter.unaryOperation(insn, value.basicValue) ?: return null
        return ConstSourceValue.Unknown(basicValue)
    }

    override fun binaryOperation(
        insn: AbstractInsnNode,
        value1: ConstSourceValue,
        value2: ConstSourceValue
    ): ConstSourceValue? {
        val basicValue = basicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue)
            ?: return null
        return ConstSourceValue.Unknown(basicValue)
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: ConstSourceValue,
        value2: ConstSourceValue,
        value3: ConstSourceValue
    ): ConstSourceValue? {
        val basicValue = basicInterpreter.ternaryOperation(
            insn,
            value1.basicValue,
            value2.basicValue,
            value3.basicValue
        ) ?: return null
        return ConstSourceValue.Unknown(basicValue)
    }

    override fun naryOperation(
        insn: AbstractInsnNode,
        values: List<ConstSourceValue>
    ): ConstSourceValue? {
        val args = values.map(ConstSourceValue::basicValue)
        val basicValue = basicInterpreter.naryOperation(insn, args) ?: return null
        return ConstSourceValue.Unknown(basicValue)
    }

    override fun returnOperation(insn: AbstractInsnNode, value: ConstSourceValue, expected: ConstSourceValue) {
        basicInterpreter.returnOperation(insn, value.basicValue, expected.basicValue)
    }

    override fun merge(value1: ConstSourceValue, value2: ConstSourceValue): ConstSourceValue {
        val basicValue = basicInterpreter.merge(value1.basicValue, value2.basicValue)
        return when {
            value1 is ConstSourceValue.Arg || value2 is ConstSourceValue.Arg -> ConstSourceValue.Arg(basicValue)
            value1 == value2 -> value1
            else -> ConstSourceValue.Unknown(basicValue)
        }
    }
}
