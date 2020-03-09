package dev.openrs2.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.LookupSwitchInsnNode
import org.objectweb.asm.tree.TableSwitchInsnNode
import org.objectweb.asm.tree.TryCatchBlockNode
import org.objectweb.asm.util.Textifier
import org.objectweb.asm.util.TraceMethodVisitor
import java.io.PrintWriter
import java.io.StringWriter

private val PURE_OPCODES = setOf(
    -1,
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
    Opcodes.BIPUSH,
    Opcodes.SIPUSH,
    Opcodes.LDC,
    Opcodes.ILOAD,
    Opcodes.LLOAD,
    Opcodes.FLOAD,
    Opcodes.DLOAD,
    Opcodes.ALOAD,
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
    /*
     * XXX(gpe): strictly speaking the *DEV and *REM instructions have side
     * effects (unless we can prove that the second argument is non-zero).
     * However, treating them as having side effects reduces the number of
     * dummy variables we can remove, so we pretend they don't have any side
     * effects.
     *
     * This doesn't seem to cause any problems with the client, as it doesn't
     * deliberately try to trigger divide-by-zero exceptions.
     */
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
    Opcodes.GETSTATIC,
    Opcodes.NEW,
    Opcodes.INSTANCEOF
)

private val IMPURE_OPCODES = setOf(
    Opcodes.IALOAD,
    Opcodes.LALOAD,
    Opcodes.FALOAD,
    Opcodes.DALOAD,
    Opcodes.AALOAD,
    Opcodes.BALOAD,
    Opcodes.CALOAD,
    Opcodes.SALOAD,
    Opcodes.ISTORE,
    Opcodes.LSTORE,
    Opcodes.FSTORE,
    Opcodes.DSTORE,
    Opcodes.ASTORE,
    Opcodes.IASTORE,
    Opcodes.LASTORE,
    Opcodes.FASTORE,
    Opcodes.DASTORE,
    Opcodes.AASTORE,
    Opcodes.BASTORE,
    Opcodes.CASTORE,
    Opcodes.SASTORE,
    Opcodes.IINC,
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
    Opcodes.RET,
    Opcodes.TABLESWITCH,
    Opcodes.LOOKUPSWITCH,
    Opcodes.IRETURN,
    Opcodes.LRETURN,
    Opcodes.FRETURN,
    Opcodes.DRETURN,
    Opcodes.ARETURN,
    Opcodes.RETURN,
    Opcodes.PUTSTATIC,
    Opcodes.GETFIELD,
    Opcodes.PUTFIELD,
    Opcodes.INVOKEVIRTUAL,
    Opcodes.INVOKESPECIAL,
    Opcodes.INVOKESTATIC,
    Opcodes.INVOKEINTERFACE,
    Opcodes.INVOKEDYNAMIC,
    Opcodes.NEWARRAY,
    Opcodes.ANEWARRAY,
    Opcodes.ARRAYLENGTH,
    Opcodes.ATHROW,
    Opcodes.CHECKCAST,
    Opcodes.MONITORENTER,
    Opcodes.MONITOREXIT,
    Opcodes.MULTIANEWARRAY,
    Opcodes.IFNULL,
    Opcodes.IFNONNULL
)

private val THROW_RETURN_OPCODES = listOf(
    Opcodes.IRETURN,
    Opcodes.LRETURN,
    Opcodes.FRETURN,
    Opcodes.DRETURN,
    Opcodes.ARETURN,
    Opcodes.RETURN,
    Opcodes.RET,
    Opcodes.ATHROW
)

val AbstractInsnNode.nextReal: AbstractInsnNode?
    get() {
        var insn = next
        while (insn != null && insn.opcode == -1) {
            insn = insn.next
        }
        return insn
    }

val AbstractInsnNode.previousReal: AbstractInsnNode?
    get() {
        var insn = previous
        while (insn != null && insn.opcode == -1) {
            insn = insn.previous
        }
        return insn
    }

val AbstractInsnNode.nextVirtual: AbstractInsnNode?
    get() {
        var insn = next
        while (insn != null && insn.opcode != -1) {
            insn = insn.next
        }
        return insn
    }

val AbstractInsnNode.previousVirtual: AbstractInsnNode?
    get() {
        var insn = previous
        while (insn != null && insn.opcode != -1) {
            insn = insn.previous
        }
        return insn
    }

val AbstractInsnNode.intConstant: Int?
    get() = when (this) {
        is IntInsnNode -> {
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                operand
            } else {
                null
            }
        }
        is LdcInsnNode -> {
            val cst = cst
            if (cst is Int) {
                cst
            } else {
                null
            }
        }
        else -> when (opcode) {
            Opcodes.ICONST_M1 -> -1
            Opcodes.ICONST_0 -> 0
            Opcodes.ICONST_1 -> 1
            Opcodes.ICONST_2 -> 2
            Opcodes.ICONST_3 -> 3
            Opcodes.ICONST_4 -> 4
            Opcodes.ICONST_5 -> 5
            else -> null
        }
    }

val AbstractInsnNode.sequential: Boolean
    get() = when (this) {
        is LabelNode -> false
        is JumpInsnNode -> false
        is TableSwitchInsnNode -> false
        is LookupSwitchInsnNode -> false
        else -> opcode !in THROW_RETURN_OPCODES
    }

val AbstractInsnNode.pure: Boolean
    get() = when (opcode) {
        in PURE_OPCODES -> true
        in IMPURE_OPCODES -> false
        else -> throw IllegalArgumentException()
    }

fun createIntConstant(value: Int): AbstractInsnNode = when (value) {
    -1 -> InsnNode(Opcodes.ICONST_M1)
    0 -> InsnNode(Opcodes.ICONST_0)
    1 -> InsnNode(Opcodes.ICONST_1)
    2 -> InsnNode(Opcodes.ICONST_2)
    3 -> InsnNode(Opcodes.ICONST_3)
    4 -> InsnNode(Opcodes.ICONST_4)
    5 -> InsnNode(Opcodes.ICONST_5)
    in Byte.MIN_VALUE..Byte.MAX_VALUE -> IntInsnNode(Opcodes.BIPUSH, value)
    in Short.MIN_VALUE..Short.MAX_VALUE -> IntInsnNode(Opcodes.SIPUSH, value)
    else -> LdcInsnNode(value)
}

fun AbstractInsnNode.toPrettyString(): String {
    val printer = Textifier()

    val visitor = TraceMethodVisitor(printer)
    accept(visitor)

    StringWriter().use { stringWriter ->
        PrintWriter(stringWriter).use { printWriter ->
            printer.print(printWriter)
            return stringWriter.toString().trim()
        }
    }
}

fun TryCatchBlockNode.isBodyEmpty(): Boolean {
    var current = start.next

    while (true) {
        when {
            current == null -> error("Failed to reach end of try-catch block.")
            current === end -> return true
            current.opcode != -1 -> return false
            else -> current = current.next
        }
    }
}
