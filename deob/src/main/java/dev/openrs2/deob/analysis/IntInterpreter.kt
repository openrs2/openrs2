package dev.openrs2.deob.analysis

import dev.openrs2.asm.intConstant
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.Interpreter

class IntInterpreter(private val args: Array<IntValueSet>) : Interpreter<IntValue>(Opcodes.ASM8) {
    private val basicInterpreter = BasicInterpreter()

    override fun newValue(type: Type?): IntValue? {
        val basicValue = basicInterpreter.newValue(type) ?: return null
        return IntValue(basicValue)
    }

    override fun newParameterValue(isInstanceMethod: Boolean, local: Int, type: Type): IntValue {
        val basicValue = basicInterpreter.newParameterValue(isInstanceMethod, local, type)

        val index = when {
            isInstanceMethod && local == 0 -> return IntValue(basicValue)
            isInstanceMethod -> local - 1
            else -> local
        }

        return IntValue(basicValue, args[index])
    }

    override fun newOperation(insn: AbstractInsnNode): IntValue {
        val basicValue = basicInterpreter.newOperation(insn)
        val v = insn.intConstant
        return if (v != null) {
            IntValue(basicValue, IntValueSet.singleton(v))
        } else {
            IntValue(basicValue)
        }
    }

    override fun copyOperation(insn: AbstractInsnNode, value: IntValue): IntValue {
        return value
    }

    override fun unaryOperation(insn: AbstractInsnNode, value: IntValue): IntValue? {
        val basicValue = basicInterpreter.unaryOperation(insn, value.basicValue) ?: return null

        if (value.set !is IntValueSet.Constant) {
            return IntValue(basicValue)
        }

        val set = mutableSetOf<Int>()
        for (v in value.set.values) {
            val result = when {
                insn.opcode == Opcodes.INEG -> -v
                insn is IincInsnNode -> v + insn.incr
                insn.opcode == Opcodes.I2B -> v.toByte().toInt()
                insn.opcode == Opcodes.I2C -> v.toChar().toInt()
                insn.opcode == Opcodes.I2S -> v.toShort().toInt()
                else -> return IntValue(basicValue)
            }
            set.add(result)
        }
        return IntValue(basicValue, IntValueSet.Constant(set))
    }

    override fun binaryOperation(insn: AbstractInsnNode, value1: IntValue, value2: IntValue): IntValue? {
        val basicValue = basicInterpreter.binaryOperation(insn, value1.basicValue, value2.basicValue) ?: return null

        if (value1.set !is IntValueSet.Constant || value2.set !is IntValueSet.Constant) {
            return IntValue(basicValue)
        }

        val set = mutableSetOf<Int>()
        for (v1 in value1.set.values) {
            for (v2 in value2.set.values) {
                val result = when (insn.opcode) {
                    Opcodes.IADD -> v1 + v2
                    Opcodes.ISUB -> v1 - v2
                    Opcodes.IMUL -> v1 * v2
                    Opcodes.IDIV -> {
                        if (v2 == 0) {
                            return IntValue(basicValue)
                        }
                        v1 / v2
                    }
                    Opcodes.IREM -> {
                        if (v2 == 0) {
                            return IntValue(basicValue)
                        }
                        v1 % v2
                    }
                    Opcodes.ISHL -> v1 shl v2
                    Opcodes.ISHR -> v1 shr v2
                    Opcodes.IUSHR -> v1 ushr v2
                    Opcodes.IAND -> v1 and v2
                    Opcodes.IOR -> v1 or v2
                    Opcodes.IXOR -> v1 xor v2
                    else -> return IntValue(basicValue)
                }
                set.add(result)
            }
        }
        return IntValue(basicValue, IntValueSet.Constant(set))
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
        return IntValue(basicValue)
    }

    override fun naryOperation(insn: AbstractInsnNode, values: List<IntValue>): IntValue? {
        val args = values.map(IntValue::basicValue)
        val basicValue = basicInterpreter.naryOperation(insn, args) ?: return null
        return IntValue(basicValue)
    }

    override fun returnOperation(insn: AbstractInsnNode, value: IntValue, expected: IntValue) {
        basicInterpreter.returnOperation(insn, value.basicValue, expected.basicValue)
    }

    override fun merge(value1: IntValue, value2: IntValue): IntValue {
        val basicValue = basicInterpreter.merge(value1.basicValue, value2.basicValue)

        if (value1 == value2) {
            return value1
        }

        if (value1.set !is IntValueSet.Constant || value2.set !is IntValueSet.Constant) {
            return IntValue(basicValue)
        }

        val set = value1.set union value2.set
        return if (set is IntValueSet.Constant && set.values.size > MAX_TRACKED_VALUES) {
            IntValue(basicValue)
        } else {
            IntValue(basicValue, set)
        }
    }

    companion object {
        private const val MAX_TRACKED_VALUES = 1
    }
}
