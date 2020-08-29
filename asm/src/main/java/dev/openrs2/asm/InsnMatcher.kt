package dev.openrs2.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.util.Printer

public class InsnMatcher private constructor(private val regex: Regex) {
    public fun match(method: MethodNode): Sequence<List<AbstractInsnNode>> {
        return match(method.instructions)
    }

    public fun match(list: InsnList): Sequence<List<AbstractInsnNode>> {
        val insns = ArrayList<AbstractInsnNode>(list.size())
        val builder = StringBuilder(list.size())

        for (instruction in list) {
            if (instruction.opcode != -1) {
                insns += instruction
                builder.append(opcodeToCodepoint(instruction.opcode))
            }
        }

        return regex.findAll(builder).map {
            insns.subList(it.range.first, it.range.last + 1)
        }
    }

    public companion object {
        private const val PRIVATE_USE_AREA = 0xE000
        private val OPCODE_GROUPS = mapOf(
            "InsnNode" to intArrayOf(
                Opcodes.NOP,
                Opcodes.ACONST_NULL,
                Opcodes.ICONST_M1,
                Opcodes.ICONST_0,
                Opcodes.ICONST_1,
                Opcodes.ICONST_2,
                Opcodes.ICONST_3,
                Opcodes.ICONST_4,
                Opcodes.ICONST_5,
                Opcodes.LCONST_0,
                Opcodes.LCONST_1,
                Opcodes.FCONST_0,
                Opcodes.FCONST_1,
                Opcodes.FCONST_2,
                Opcodes.DCONST_0,
                Opcodes.DCONST_1,
                Opcodes.IALOAD,
                Opcodes.LALOAD,
                Opcodes.FALOAD,
                Opcodes.DALOAD,
                Opcodes.AALOAD,
                Opcodes.BALOAD,
                Opcodes.CALOAD,
                Opcodes.SALOAD,
                Opcodes.IASTORE,
                Opcodes.LASTORE,
                Opcodes.FASTORE,
                Opcodes.DASTORE,
                Opcodes.AASTORE,
                Opcodes.BASTORE,
                Opcodes.CASTORE,
                Opcodes.SASTORE,
                Opcodes.POP,
                Opcodes.POP2,
                Opcodes.DUP,
                Opcodes.DUP_X1,
                Opcodes.DUP_X2,
                Opcodes.DUP2,
                Opcodes.DUP2_X1,
                Opcodes.DUP2_X2,
                Opcodes.SWAP,
                Opcodes.IADD,
                Opcodes.LADD,
                Opcodes.FADD,
                Opcodes.DADD,
                Opcodes.ISUB,
                Opcodes.LSUB,
                Opcodes.FSUB,
                Opcodes.DSUB,
                Opcodes.IMUL,
                Opcodes.LMUL,
                Opcodes.FMUL,
                Opcodes.DMUL,
                Opcodes.IDIV,
                Opcodes.LDIV,
                Opcodes.FDIV,
                Opcodes.DDIV,
                Opcodes.IREM,
                Opcodes.LREM,
                Opcodes.FREM,
                Opcodes.DREM,
                Opcodes.INEG,
                Opcodes.LNEG,
                Opcodes.FNEG,
                Opcodes.DNEG,
                Opcodes.ISHL,
                Opcodes.LSHL,
                Opcodes.ISHR,
                Opcodes.LSHR,
                Opcodes.IUSHR,
                Opcodes.LUSHR,
                Opcodes.IAND,
                Opcodes.LAND,
                Opcodes.IOR,
                Opcodes.LOR,
                Opcodes.IXOR,
                Opcodes.LXOR,
                Opcodes.I2L,
                Opcodes.I2F,
                Opcodes.I2D,
                Opcodes.L2I,
                Opcodes.L2F,
                Opcodes.L2D,
                Opcodes.F2I,
                Opcodes.F2L,
                Opcodes.F2D,
                Opcodes.D2I,
                Opcodes.D2L,
                Opcodes.D2F,
                Opcodes.I2B,
                Opcodes.I2C,
                Opcodes.I2S,
                Opcodes.LCMP,
                Opcodes.FCMPL,
                Opcodes.FCMPG,
                Opcodes.DCMPL,
                Opcodes.DCMPG,
                Opcodes.IRETURN,
                Opcodes.LRETURN,
                Opcodes.FRETURN,
                Opcodes.DRETURN,
                Opcodes.ARETURN,
                Opcodes.RETURN,
                Opcodes.ARRAYLENGTH,
                Opcodes.ATHROW,
                Opcodes.MONITORENTER,
                Opcodes.MONITOREXIT
            ),
            "IntInsnNode" to intArrayOf(
                Opcodes.BIPUSH,
                Opcodes.SIPUSH,
                Opcodes.NEWARRAY
            ),
            "VarInsnNode" to intArrayOf(
                Opcodes.ILOAD,
                Opcodes.LLOAD,
                Opcodes.FLOAD,
                Opcodes.DLOAD,
                Opcodes.ALOAD,
                Opcodes.ISTORE,
                Opcodes.LSTORE,
                Opcodes.FSTORE,
                Opcodes.DSTORE,
                Opcodes.ASTORE,
                Opcodes.RET
            ),
            "TypeInsnNode" to intArrayOf(
                Opcodes.NEW,
                Opcodes.ANEWARRAY,
                Opcodes.CHECKCAST,
                Opcodes.INSTANCEOF
            ),
            "FieldInsnNode" to intArrayOf(
                Opcodes.GETSTATIC,
                Opcodes.PUTSTATIC,
                Opcodes.GETFIELD,
                Opcodes.PUTFIELD
            ),
            "MethodInsnNode" to intArrayOf(
                Opcodes.INVOKEVIRTUAL,
                Opcodes.INVOKESPECIAL,
                Opcodes.INVOKESTATIC,
                Opcodes.INVOKEINTERFACE
            ),
            "InvokeDynamicInsnNode" to intArrayOf(
                Opcodes.INVOKEDYNAMIC
            ),
            "JumpInsnNode" to intArrayOf(
                Opcodes.IFEQ,
                Opcodes.IFNE,
                Opcodes.IFLT,
                Opcodes.IFGE,
                Opcodes.IFGT,
                Opcodes.IFLE,
                Opcodes.IF_ICMPEQ,
                Opcodes.IF_ICMPNE,
                Opcodes.IF_ICMPLT,
                Opcodes.IF_ICMPGE,
                Opcodes.IF_ICMPGT,
                Opcodes.IF_ICMPLE,
                Opcodes.IF_ACMPEQ,
                Opcodes.IF_ACMPNE,
                Opcodes.GOTO,
                Opcodes.JSR,
                Opcodes.IFNULL,
                Opcodes.IFNONNULL
            ),
            "LdcInsnNode" to intArrayOf(
                Opcodes.LDC
            ),
            "IincInsnNode" to intArrayOf(
                Opcodes.IINC
            ),
            "TableSwitchInsnNode" to intArrayOf(
                Opcodes.TABLESWITCH
            ),
            "LookupSwitchInsnNode" to intArrayOf(
                Opcodes.LOOKUPSWITCH
            ),
            "MultiANewArrayInsnNode" to intArrayOf(
                Opcodes.MULTIANEWARRAY
            ),
            "ICONST" to intArrayOf(
                Opcodes.ICONST_M1,
                Opcodes.ICONST_0,
                Opcodes.ICONST_1,
                Opcodes.ICONST_2,
                Opcodes.ICONST_3,
                Opcodes.ICONST_4,
                Opcodes.ICONST_5
            ),
            "FCONST" to intArrayOf(
                Opcodes.FCONST_0,
                Opcodes.FCONST_1,
                Opcodes.FCONST_2
            ),
            "DCONST" to intArrayOf(
                Opcodes.DCONST_0,
                Opcodes.DCONST_1
            )
        )

        private fun opcodeToCodepoint(opcode: Int): Char {
            return (PRIVATE_USE_AREA + opcode).toChar()
        }

        private fun appendOpcodeRegex(pattern: StringBuilder, opcode: String) {
            val i = Printer.OPCODES.indexOf(opcode)
            if (i != -1) {
                pattern.append(opcodeToCodepoint(i))
                return
            }

            val group = OPCODE_GROUPS[opcode]
            if (group != null) {
                pattern.append('(')
                group.map { opcodeToCodepoint(it) }.joinTo(pattern, "|")
                pattern.append(')')
                return
            }

            if (opcode == "AbstractInsnNode") {
                pattern.append('.')
                return
            }

            throw IllegalArgumentException("$opcode is not a valid opcode or opcode group")
        }

        public fun compile(regex: String): InsnMatcher {
            val pattern = StringBuilder()
            val opcode = StringBuilder()

            for (c in regex) {
                if (c.isLetterOrDigit() || c == '_') {
                    opcode.append(c)
                } else {
                    if (opcode.isNotEmpty()) {
                        appendOpcodeRegex(pattern, opcode.toString())
                        opcode.delete(0, opcode.length)
                    }

                    if (!c.isWhitespace()) {
                        pattern.append(c)
                    }
                }
            }

            if (opcode.isNotEmpty()) {
                appendOpcodeRegex(pattern, opcode.toString())
                opcode.delete(0, opcode.length)
            }

            return InsnMatcher(Regex(pattern.toString()))
        }
    }
}
