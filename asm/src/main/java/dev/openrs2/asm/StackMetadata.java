package dev.openrs2.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;

public final class StackMetadata {
	private static final StackMetadata NONE = new StackMetadata(0, 0);
	private static final StackMetadata POP1 = new StackMetadata(1, 0);
	private static final StackMetadata POP1_PUSH1 = new StackMetadata(1, 1);
	private static final StackMetadata POP1_PUSH2 = new StackMetadata(1, 2);
	private static final StackMetadata POP2 = new StackMetadata(2, 0);
	private static final StackMetadata POP2_PUSH1 = new StackMetadata(2, 1);
	private static final StackMetadata POP2_PUSH2 = new StackMetadata(2, 2);
	private static final StackMetadata POP2_PUSH3 = new StackMetadata(2, 3);
	private static final StackMetadata POP2_PUSH4 = new StackMetadata(2, 4);
	private static final StackMetadata POP3 = new StackMetadata(3, 0);
	private static final StackMetadata POP3_PUSH2 = new StackMetadata(3, 2);
	private static final StackMetadata POP3_PUSH4 = new StackMetadata(3, 4);
	private static final StackMetadata POP3_PUSH5 = new StackMetadata(3, 5);
	private static final StackMetadata POP4 = new StackMetadata(4, 0);
	private static final StackMetadata POP4_PUSH1 = new StackMetadata(4, 1);
	private static final StackMetadata POP4_PUSH2 = new StackMetadata(4, 2);
	private static final StackMetadata POP4_PUSH6 = new StackMetadata(4, 6);
	private static final StackMetadata PUSH1 = new StackMetadata(0, 1);
	private static final StackMetadata PUSH2 = new StackMetadata(0, 2);

	public static StackMetadata get(AbstractInsnNode insn) {
		var opcode = insn.getOpcode();
		switch (opcode) {
		case Opcodes.NOP:
			return NONE;
		case Opcodes.ACONST_NULL:
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
			return PUSH1;
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
			return PUSH2;
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
			return PUSH1;
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
			return PUSH2;
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			return PUSH1;
		case Opcodes.LDC:
			var cst = ((LdcInsnNode) insn).cst;
			if (cst instanceof Double || cst instanceof Long) {
				return PUSH2;
			} else {
				return PUSH1;
			}
		case Opcodes.ILOAD:
			return PUSH1;
		case Opcodes.LLOAD:
			return PUSH2;
		case Opcodes.FLOAD:
			return PUSH1;
		case Opcodes.DLOAD:
			return PUSH2;
		case Opcodes.ALOAD:
			return PUSH1;
		case Opcodes.IALOAD:
			return POP2_PUSH1;
		case Opcodes.LALOAD:
			return POP2_PUSH2;
		case Opcodes.FALOAD:
			return POP2_PUSH1;
		case Opcodes.DALOAD:
			return POP2_PUSH2;
		case Opcodes.AALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
			return POP2_PUSH1;
		case Opcodes.ISTORE:
			return POP1;
		case Opcodes.LSTORE:
			return POP2;
		case Opcodes.FSTORE:
			return POP1;
		case Opcodes.DSTORE:
			return POP2;
		case Opcodes.ASTORE:
			return POP1;
		case Opcodes.IASTORE:
			return POP3;
		case Opcodes.LASTORE:
			return POP4;
		case Opcodes.FASTORE:
			return POP3;
		case Opcodes.DASTORE:
			return POP4;
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			return POP3;
		case Opcodes.POP:
			return POP1;
		case Opcodes.POP2:
			return POP2;
		case Opcodes.DUP:
			return POP1_PUSH2;
		case Opcodes.DUP_X1:
			return POP2_PUSH3;
		case Opcodes.DUP_X2:
			return POP3_PUSH4;
		case Opcodes.DUP2:
			return POP2_PUSH4;
		case Opcodes.DUP2_X1:
			return POP3_PUSH5;
		case Opcodes.DUP2_X2:
			return POP4_PUSH6;
		case Opcodes.SWAP:
			return POP2_PUSH2;
		case Opcodes.IADD:
			return POP2_PUSH1;
		case Opcodes.LADD:
			return POP4_PUSH2;
		case Opcodes.FADD:
			return POP2_PUSH1;
		case Opcodes.DADD:
			return POP4_PUSH2;
		case Opcodes.ISUB:
			return POP2_PUSH1;
		case Opcodes.LSUB:
			return POP4_PUSH2;
		case Opcodes.FSUB:
			return POP2_PUSH1;
		case Opcodes.DSUB:
			return POP4_PUSH2;
		case Opcodes.IMUL:
			return POP2_PUSH1;
		case Opcodes.LMUL:
			return POP4_PUSH2;
		case Opcodes.FMUL:
			return POP2_PUSH1;
		case Opcodes.DMUL:
			return POP4_PUSH2;
		case Opcodes.IDIV:
			return POP2_PUSH1;
		case Opcodes.LDIV:
			return POP4_PUSH2;
		case Opcodes.FDIV:
			return POP2_PUSH1;
		case Opcodes.DDIV:
			return POP4_PUSH2;
		case Opcodes.IREM:
			return POP2_PUSH1;
		case Opcodes.LREM:
			return POP4_PUSH2;
		case Opcodes.FREM:
			return POP2_PUSH1;
		case Opcodes.DREM:
			return POP4_PUSH2;
		case Opcodes.INEG:
			return POP1_PUSH1;
		case Opcodes.LNEG:
			return POP2_PUSH2;
		case Opcodes.FNEG:
			return POP1_PUSH1;
		case Opcodes.DNEG:
			return POP2_PUSH2;
		case Opcodes.ISHL:
			return POP2_PUSH1;
		case Opcodes.LSHL:
			return POP3_PUSH2;
		case Opcodes.ISHR:
			return POP2_PUSH1;
		case Opcodes.LSHR:
			return POP3_PUSH2;
		case Opcodes.IUSHR:
			return POP2_PUSH1;
		case Opcodes.LUSHR:
			return POP3_PUSH2;
		case Opcodes.IAND:
			return POP2_PUSH1;
		case Opcodes.LAND:
			return POP4_PUSH2;
		case Opcodes.IOR:
			return POP2_PUSH1;
		case Opcodes.LOR:
			return POP4_PUSH2;
		case Opcodes.IXOR:
			return POP2_PUSH1;
		case Opcodes.LXOR:
			return POP4_PUSH2;
		case Opcodes.IINC:
			return NONE;
		case Opcodes.I2L:
			return POP1_PUSH2;
		case Opcodes.I2F:
			return POP1_PUSH1;
		case Opcodes.I2D:
			return POP1_PUSH2;
		case Opcodes.L2I:
			return POP2_PUSH1;
		case Opcodes.L2F:
			return POP2_PUSH1;
		case Opcodes.L2D:
			return POP2_PUSH2;
		case Opcodes.F2I:
			return POP1_PUSH1;
		case Opcodes.F2L:
			return POP1_PUSH2;
		case Opcodes.F2D:
			return POP1_PUSH2;
		case Opcodes.D2I:
			return POP2_PUSH1;
		case Opcodes.D2L:
			return POP2_PUSH2;
		case Opcodes.D2F:
			return POP2_PUSH1;
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
			return POP1_PUSH1;
		case Opcodes.LCMP:
			return POP4_PUSH1;
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
			return POP2_PUSH1;
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
			return POP4_PUSH1;
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
			return POP1;
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
			return POP2;
		case Opcodes.GOTO:
			return NONE;
		case Opcodes.JSR:
			return PUSH1;
		case Opcodes.RET:
			return NONE;
		case Opcodes.TABLESWITCH:
		case Opcodes.LOOKUPSWITCH:
			return POP1;
		case Opcodes.IRETURN:
			return POP1;
		case Opcodes.LRETURN:
			return POP2;
		case Opcodes.FRETURN:
			return POP1;
		case Opcodes.DRETURN:
			return POP2;
		case Opcodes.ARETURN:
			return POP1;
		case Opcodes.RETURN:
			return NONE;
		case Opcodes.GETSTATIC:
		case Opcodes.PUTSTATIC:
		case Opcodes.GETFIELD:
		case Opcodes.PUTFIELD:
			var fieldInsn = (FieldInsnNode) insn;
			var fieldSize = Type.getType(fieldInsn.desc).getSize();

			int pushes = 0, pops = 0;

			if (opcode == Opcodes.GETFIELD || opcode == Opcodes.PUTFIELD) {
				pops++;
			}

			if (opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC) {
				pushes += fieldSize;
			} else {
				pops += fieldSize;
			}

			return new StackMetadata(pops, pushes);
		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.INVOKESPECIAL:
		case Opcodes.INVOKESTATIC:
		case Opcodes.INVOKEINTERFACE:
			var methodInsn = (MethodInsnNode) insn;
			var argumentsAndReturnSizes = Type.getArgumentsAndReturnSizes(methodInsn.desc);

			pushes = argumentsAndReturnSizes >> 2;
			pops = argumentsAndReturnSizes & 0x3;

			if (opcode != Opcodes.INVOKESTATIC) {
				pops++;
			}

			return new StackMetadata(pops, pushes);
		case Opcodes.INVOKEDYNAMIC:
			throw new UnsupportedOperationException();
		case Opcodes.NEW:
			return POP1;
		case Opcodes.NEWARRAY:
		case Opcodes.ANEWARRAY:
		case Opcodes.ARRAYLENGTH:
			return POP1_PUSH1;
		case Opcodes.ATHROW:
			return POP1;
		case Opcodes.CHECKCAST:
		case Opcodes.INSTANCEOF:
			return POP1_PUSH1;
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
			return POP1;
		case Opcodes.MULTIANEWARRAY:
			return new StackMetadata(((MultiANewArrayInsnNode) insn).dims, 1);
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:
			return POP1;
		}

		throw new IllegalArgumentException();
	}

	private final int pops, pushes;

	private StackMetadata(int pops, int pushes) {
		this.pops = pops;
		this.pushes = pushes;
	}

	public int getPops() {
		return pops;
	}

	public int getPushes() {
		return pushes;
	}
}
