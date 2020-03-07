package dev.openrs2.deob.analysis

import dev.openrs2.asm.intConstant
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Interpreter

class IntInterpreter(private val parameters: Array<Set<Int>?>?) : Interpreter<IntValue>(Opcodes.ASM7) {
    private val basicInterpreter = BasicInterpreter()

    override fun newValue(type: Type?): IntValue? {
        val basicValue = basicInterpreter.newValue(type) ?: return null
        return IntValue.Unknown(basicValue)
    }

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): IntValue {
        val basicValue = basicInterpreter.newParameterValue(isInstanceMethod, local, type)

        if (parameters != null) {
            val parameterIndex = when {
                isInstanceMethod && local == 0 -> return IntValue.Unknown(basicValue)
                isInstanceMethod -> local - 1
                else -> local
            }

            val parameter = parameters[parameterIndex]
            if (parameter != null) {
                return IntValue.Constant(basicValue, parameter)
            }
        }

        return IntValue.Unknown(basicValue)
    }

    override fun newOperation(insn: AbstractInsnNode): IntValue {
        val basicValue = basicInterpreter.newOperation(insn)
        val v = insn.intConstant
        return if (v != null) {
            IntValue.Constant(basicValue, v)
        } else {
            IntValue.Unknown(basicValue)
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: IntValue): IntValue {
        return value
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: IntValue): IntValue? {
        val basicValue = basicInterpreter.unaryOperation(insn, value.basicValue) ?: return null

        if (value !is IntValue.Constant) {
            return IntValue.Unknown(basicValue)
        }

        val set = mutableSetOf<Int>()
        for (v in value.values) {
            val result = when {
                insn.opcode == Opcodes.INEG -> -v
                insn is IincInsnNode -> v + insn.incr
                insn.opcode == Opcodes.I2B -> v.toByte().toInt()
                insn.opcode == Opcodes.I2C -> v.toChar().toInt()
                insn.opcode == Opcodes.I2S -> v.toShort().toInt()
                else -> return IntValue.Unknown(basicValue)
            }
            set.add(result)
        }
        return IntValue.Constant(basicValue, set)
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: IntValue, value2: IntValue): IntValue? {
        val basicValue = basicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue) ?: return null

        if (value1 !is IntValue.Constant || value2 !is IntValue.Constant) {
            return IntValue.Unknown(basicValue)
        }

        val set = mutableSetOf<Int>()
        for (v1 in value1.values) {
            for (v2 in value2.values) {
                val result = when (insn.opcode) {
                    Opcodes.IADD -> v1 + v2
                    Opcodes.ISUB -> v1 - v2
                    Opcodes.IMUL -> v1 * v2
                    Opcodes.IDIV -> {
                        if (v2 == 0) {
                            return IntValue.Unknown(basicValue)
                        }
                        v1 / v2
                    }
                    Opcodes.IREM -> {
                        if (v2 == 0) {
                            return IntValue.Unknown(basicValue)
                        }
                        v1 % v2
                    }
                    Opcodes.ISHL -> v1 shl v2
                    Opcodes.ISHR -> v1 shr v2
                    Opcodes.IUSHR -> v1 ushr v2
                    Opcodes.IAND -> v1 and v2
                    Opcodes.IOR -> v1 or v2
                    Opcodes.IXOR -> v1 xor v2
                    else -> return IntValue.Unknown(basicValue)
                }
                set.add(result)
            }
        }
        return IntValue.Constant(basicValue, set)
    }

    override fun ternaryOperation(
        insn: AbstractInsnNode,
        value1: IntValue,
        value2: IntValue,
        value3: IntValue
    ): IntValue? {
        val basicValue =
            basicInterpreter.ternaryOperation(insn, value1.basicValue, value2.basicValue, value3.basicValue)
                ?: return null
        return IntValue.Unknown(basicValue)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<IntValue>): IntValue? {
        val args = values.map(IntValue::basicValue)
        val basicValue = basicInterpreter.naryOperation(insn, args) ?: return null
        return IntValue.Unknown(basicValue)
    }

    override fun returnOperation(insn: AbstractInsnNode, value: IntValue, expected: IntValue) {
        basicInterpreter.returnOperation(insn, value.basicValue, expected.basicValue)
    }

    override fun merge(value1: IntValue, value2: IntValue): IntValue {
        val basicValue = basicInterpreter.merge(value1.basicValue, value2.basicValue)

        if (value1 == value2) {
            return value1
        }

        if (value1 !is IntValue.Constant || value2 !is IntValue.Constant) {
            return IntValue.Unknown(basicValue)
        }

        val set = value1.values union value2.values
        return if (set.size > MAX_TRACKED_VALUES) {
            IntValue.Unknown(basicValue)
        } else {
            IntValue.Constant(basicValue, set)
        }
    }

    companion object {
        private const val MAX_TRACKED_VALUES = 1
    }
}
