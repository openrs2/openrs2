package org.openrs2.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode

public data class StackMetadata(val pops: Int, val pushes: Int)

private val NONE = StackMetadata(0, 0)
private val POP1 = StackMetadata(1, 0)
private val POP1_PUSH1 = StackMetadata(1, 1)
private val POP1_PUSH2 = StackMetadata(1, 2)
private val POP2 = StackMetadata(2, 0)
private val POP2_PUSH1 = StackMetadata(2, 1)
private val POP2_PUSH2 = StackMetadata(2, 2)
private val POP2_PUSH3 = StackMetadata(2, 3)
private val POP2_PUSH4 = StackMetadata(2, 4)
private val POP3 = StackMetadata(3, 0)
private val POP3_PUSH2 = StackMetadata(3, 2)
private val POP3_PUSH4 = StackMetadata(3, 4)
private val POP3_PUSH5 = StackMetadata(3, 5)
private val POP4 = StackMetadata(4, 0)
private val POP4_PUSH1 = StackMetadata(4, 1)
private val POP4_PUSH2 = StackMetadata(4, 2)
private val POP4_PUSH6 = StackMetadata(4, 6)
private val PUSH1 = StackMetadata(0, 1)
private val PUSH2 = StackMetadata(0, 2)

private val SIMPLE_OPCODES = mapOf(
    -1 to NONE,
    Opcodes.NOP to NONE,
    Opcodes.ACONST_NULL to PUSH1,
    Opcodes.ICONST_M1 to PUSH1,
    Opcodes.ICONST_0 to PUSH1,
    Opcodes.ICONST_1 to PUSH1,
    Opcodes.ICONST_2 to PUSH1,
    Opcodes.ICONST_3 to PUSH1,
    Opcodes.ICONST_4 to PUSH1,
    Opcodes.ICONST_5 to PUSH1,
    Opcodes.LCONST_0 to PUSH2,
    Opcodes.LCONST_1 to PUSH2,
    Opcodes.FCONST_0 to PUSH1,
    Opcodes.FCONST_1 to PUSH1,
    Opcodes.FCONST_2 to PUSH1,
    Opcodes.DCONST_0 to PUSH2,
    Opcodes.DCONST_1 to PUSH2,
    Opcodes.BIPUSH to PUSH1,
    Opcodes.SIPUSH to PUSH1,
    Opcodes.ILOAD to PUSH1,
    Opcodes.LLOAD to PUSH2,
    Opcodes.FLOAD to PUSH1,
    Opcodes.DLOAD to PUSH2,
    Opcodes.ALOAD to PUSH1,
    Opcodes.IALOAD to POP2_PUSH1,
    Opcodes.LALOAD to POP2_PUSH2,
    Opcodes.FALOAD to POP2_PUSH1,
    Opcodes.DALOAD to POP2_PUSH2,
    Opcodes.AALOAD to POP2_PUSH1,
    Opcodes.BALOAD to POP2_PUSH1,
    Opcodes.CALOAD to POP2_PUSH1,
    Opcodes.SALOAD to POP2_PUSH1,
    Opcodes.ISTORE to POP1,
    Opcodes.LSTORE to POP2,
    Opcodes.FSTORE to POP1,
    Opcodes.DSTORE to POP2,
    Opcodes.ASTORE to POP1,
    Opcodes.IASTORE to POP3,
    Opcodes.LASTORE to POP4,
    Opcodes.FASTORE to POP3,
    Opcodes.DASTORE to POP4,
    Opcodes.AASTORE to POP3,
    Opcodes.BASTORE to POP3,
    Opcodes.CASTORE to POP3,
    Opcodes.SASTORE to POP3,
    Opcodes.POP to POP1,
    Opcodes.POP2 to POP2,
    Opcodes.DUP to POP1_PUSH2,
    Opcodes.DUP_X1 to POP2_PUSH3,
    Opcodes.DUP_X2 to POP3_PUSH4,
    Opcodes.DUP2 to POP2_PUSH4,
    Opcodes.DUP2_X1 to POP3_PUSH5,
    Opcodes.DUP2_X2 to POP4_PUSH6,
    Opcodes.SWAP to POP2_PUSH2,
    Opcodes.IADD to POP2_PUSH1,
    Opcodes.LADD to POP4_PUSH2,
    Opcodes.FADD to POP2_PUSH1,
    Opcodes.DADD to POP4_PUSH2,
    Opcodes.ISUB to POP2_PUSH1,
    Opcodes.LSUB to POP4_PUSH2,
    Opcodes.FSUB to POP2_PUSH1,
    Opcodes.DSUB to POP4_PUSH2,
    Opcodes.IMUL to POP2_PUSH1,
    Opcodes.LMUL to POP4_PUSH2,
    Opcodes.FMUL to POP2_PUSH1,
    Opcodes.DMUL to POP4_PUSH2,
    Opcodes.IDIV to POP2_PUSH1,
    Opcodes.LDIV to POP4_PUSH2,
    Opcodes.FDIV to POP2_PUSH1,
    Opcodes.DDIV to POP4_PUSH2,
    Opcodes.IREM to POP2_PUSH1,
    Opcodes.LREM to POP4_PUSH2,
    Opcodes.FREM to POP2_PUSH1,
    Opcodes.DREM to POP4_PUSH2,
    Opcodes.INEG to POP1_PUSH1,
    Opcodes.LNEG to POP2_PUSH2,
    Opcodes.FNEG to POP1_PUSH1,
    Opcodes.DNEG to POP2_PUSH2,
    Opcodes.ISHL to POP2_PUSH1,
    Opcodes.LSHL to POP3_PUSH2,
    Opcodes.ISHR to POP2_PUSH1,
    Opcodes.LSHR to POP3_PUSH2,
    Opcodes.IUSHR to POP2_PUSH1,
    Opcodes.LUSHR to POP3_PUSH2,
    Opcodes.IAND to POP2_PUSH1,
    Opcodes.LAND to POP4_PUSH2,
    Opcodes.IOR to POP2_PUSH1,
    Opcodes.LOR to POP4_PUSH2,
    Opcodes.IXOR to POP2_PUSH1,
    Opcodes.LXOR to POP4_PUSH2,
    Opcodes.IINC to NONE,
    Opcodes.I2L to POP1_PUSH2,
    Opcodes.I2F to POP1_PUSH1,
    Opcodes.I2D to POP1_PUSH2,
    Opcodes.L2I to POP2_PUSH1,
    Opcodes.L2F to POP2_PUSH1,
    Opcodes.L2D to POP2_PUSH2,
    Opcodes.F2I to POP1_PUSH1,
    Opcodes.F2L to POP1_PUSH2,
    Opcodes.F2D to POP1_PUSH2,
    Opcodes.D2I to POP2_PUSH1,
    Opcodes.D2L to POP2_PUSH2,
    Opcodes.D2F to POP2_PUSH1,
    Opcodes.I2B to POP1_PUSH1,
    Opcodes.I2C to POP1_PUSH1,
    Opcodes.I2S to POP1_PUSH1,
    Opcodes.LCMP to POP4_PUSH1,
    Opcodes.FCMPL to POP2_PUSH1,
    Opcodes.FCMPG to POP2_PUSH1,
    Opcodes.DCMPL to POP4_PUSH1,
    Opcodes.DCMPG to POP4_PUSH1,
    Opcodes.IFEQ to POP1,
    Opcodes.IFNE to POP1,
    Opcodes.IFLT to POP1,
    Opcodes.IFGE to POP1,
    Opcodes.IFGT to POP1,
    Opcodes.IFLE to POP1,
    Opcodes.IF_ICMPEQ to POP2,
    Opcodes.IF_ICMPNE to POP2,
    Opcodes.IF_ICMPLT to POP2,
    Opcodes.IF_ICMPGE to POP2,
    Opcodes.IF_ICMPGT to POP2,
    Opcodes.IF_ICMPLE to POP2,
    Opcodes.IF_ACMPEQ to POP2,
    Opcodes.IF_ACMPNE to POP2,
    Opcodes.GOTO to NONE,
    Opcodes.JSR to PUSH1,
    Opcodes.RET to NONE,
    Opcodes.TABLESWITCH to POP1,
    Opcodes.LOOKUPSWITCH to POP1,
    Opcodes.IRETURN to POP1,
    Opcodes.LRETURN to POP2,
    Opcodes.FRETURN to POP1,
    Opcodes.DRETURN to POP2,
    Opcodes.ARETURN to POP1,
    Opcodes.RETURN to NONE,
    Opcodes.NEW to PUSH1,
    Opcodes.NEWARRAY to POP1_PUSH1,
    Opcodes.ANEWARRAY to POP1_PUSH1,
    Opcodes.ARRAYLENGTH to POP1_PUSH1,
    Opcodes.ATHROW to POP1,
    Opcodes.CHECKCAST to POP1_PUSH1,
    Opcodes.INSTANCEOF to POP1_PUSH1,
    Opcodes.MONITORENTER to POP1,
    Opcodes.MONITOREXIT to POP1,
    Opcodes.IFNULL to POP1,
    Opcodes.IFNONNULL to POP1
)

public val AbstractInsnNode.stackMetadata: StackMetadata
    get() = when (this) {
        is LdcInsnNode -> if (cst is Double || cst is Long) {
            PUSH2
        } else {
            PUSH1
        }
        is FieldInsnNode -> {
            val fieldSize = Type.getType(desc).size
            var pushes = 0
            var pops = 0
            if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
                pops++
            }
            if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
                pops += fieldSize
            } else {
                pushes += fieldSize
            }
            StackMetadata(pops, pushes)
        }
        is MethodInsnNode -> {
            val argumentsAndReturnSizes = Type.getArgumentsAndReturnSizes(desc)
            val pushes = argumentsAndReturnSizes and 0x3
            var pops = argumentsAndReturnSizes shr 2
            if (opcode == Opcodes.INVOKESTATIC) {
                pops--
            }
            StackMetadata(pops, pushes)
        }
        is InvokeDynamicInsnNode -> throw UnsupportedOperationException()
        is MultiANewArrayInsnNode -> StackMetadata(dims, 1)
        else -> SIMPLE_OPCODES[opcode] ?: throw IllegalArgumentException()
    }
