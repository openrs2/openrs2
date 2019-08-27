package dev.openrs2.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;

public final class InsnNodeUtils {
	public static AbstractInsnNode nextReal(AbstractInsnNode insn) {
		while ((insn = insn.getNext()) != null && insn.getOpcode() == -1) {
			/* empty */
		}
		return insn;
	}

	public static AbstractInsnNode previousReal(AbstractInsnNode insn) {
		while ((insn = insn.getPrevious()) != null && insn.getOpcode() == -1) {
			/* empty */
		}
		return insn;
	}

	public static AbstractInsnNode nextVirtual(AbstractInsnNode insn) {
		while ((insn = insn.getNext()) != null && insn.getOpcode() != -1) {
			/* empty */
		}
		return insn;
	}

	public static AbstractInsnNode previousVirtual(AbstractInsnNode insn) {
		while ((insn = insn.getPrevious()) != null && insn.getOpcode() != -1) {
			/* empty */
		}
		return insn;
	}

	public static boolean isIntConstant(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			return true;
		case Opcodes.LDC:
			var ldc = (LdcInsnNode) insn;
			return ldc.cst instanceof Integer;
		}

		return false;
	}

	public static int getIntConstant(AbstractInsnNode insn) {
		switch (insn.getOpcode()) {
		case Opcodes.ICONST_M1:
			return -1;
		case Opcodes.ICONST_0:
			return 0;
		case Opcodes.ICONST_1:
			return 1;
		case Opcodes.ICONST_2:
			return 2;
		case Opcodes.ICONST_3:
			return 3;
		case Opcodes.ICONST_4:
			return 4;
		case Opcodes.ICONST_5:
			return 5;
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
			var intInsn = (IntInsnNode) insn;
			return intInsn.operand;
		case Opcodes.LDC:
			var ldc = (LdcInsnNode) insn;
			if (ldc.cst instanceof Integer) {
				return (Integer) ldc.cst;
			}
		}

		throw new IllegalArgumentException();
	}

	public static AbstractInsnNode createIntConstant(int value) {
		switch (value) {
		case -1:
			return new InsnNode(Opcodes.ICONST_M1);
		case 0:
			return new InsnNode(Opcodes.ICONST_0);
		case 1:
			return new InsnNode(Opcodes.ICONST_1);
		case 2:
			return new InsnNode(Opcodes.ICONST_2);
		case 3:
			return new InsnNode(Opcodes.ICONST_3);
		case 4:
			return new InsnNode(Opcodes.ICONST_4);
		case 5:
			return new InsnNode(Opcodes.ICONST_5);
		}

		if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) {
			return new IntInsnNode(Opcodes.BIPUSH, value);
		} else if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) {
			return new IntInsnNode(Opcodes.SIPUSH, value);
		} else {
			return new LdcInsnNode(value);
		}
	}

	public static boolean hasSideEffects(AbstractInsnNode insn) {
		var opcode = insn.getOpcode();
		switch (opcode) {
		case -1:
		case Opcodes.NOP:
		case Opcodes.ACONST_NULL:
		case Opcodes.ICONST_M1:
		case Opcodes.ICONST_0:
		case Opcodes.ICONST_1:
		case Opcodes.ICONST_2:
		case Opcodes.ICONST_3:
		case Opcodes.ICONST_4:
		case Opcodes.ICONST_5:
		case Opcodes.LCONST_0:
		case Opcodes.LCONST_1:
		case Opcodes.FCONST_0:
		case Opcodes.FCONST_1:
		case Opcodes.FCONST_2:
		case Opcodes.DCONST_0:
		case Opcodes.DCONST_1:
		case Opcodes.BIPUSH:
		case Opcodes.SIPUSH:
		case Opcodes.LDC:
		case Opcodes.ILOAD:
		case Opcodes.LLOAD:
		case Opcodes.FLOAD:
		case Opcodes.DLOAD:
		case Opcodes.ALOAD:
			return false;
		case Opcodes.IALOAD:
		case Opcodes.LALOAD:
		case Opcodes.FALOAD:
		case Opcodes.DALOAD:
		case Opcodes.AALOAD:
		case Opcodes.BALOAD:
		case Opcodes.CALOAD:
		case Opcodes.SALOAD:
			/* might throw NPE or index out of bounds exception */
		case Opcodes.ISTORE:
		case Opcodes.LSTORE:
		case Opcodes.FSTORE:
		case Opcodes.DSTORE:
		case Opcodes.ASTORE:
		case Opcodes.IASTORE:
		case Opcodes.LASTORE:
		case Opcodes.FASTORE:
		case Opcodes.DASTORE:
		case Opcodes.AASTORE:
		case Opcodes.BASTORE:
		case Opcodes.CASTORE:
		case Opcodes.SASTORE:
			return true;
		case Opcodes.POP:
		case Opcodes.POP2:
		case Opcodes.DUP:
		case Opcodes.DUP_X1:
		case Opcodes.DUP_X2:
		case Opcodes.DUP2:
		case Opcodes.DUP2_X1:
		case Opcodes.DUP2_X2:
		case Opcodes.SWAP:
		case Opcodes.IADD:
		case Opcodes.LADD:
		case Opcodes.FADD:
		case Opcodes.DADD:
		case Opcodes.ISUB:
		case Opcodes.LSUB:
		case Opcodes.FSUB:
		case Opcodes.DSUB:
		case Opcodes.IMUL:
		case Opcodes.LMUL:
		case Opcodes.FMUL:
		case Opcodes.DMUL:
		case Opcodes.IDIV:
		case Opcodes.LDIV:
		case Opcodes.FDIV:
		case Opcodes.DDIV:
		case Opcodes.IREM:
		case Opcodes.LREM:
		case Opcodes.FREM:
		case Opcodes.DREM:
			/*
			 * XXX(gpe): strictly speaking the *DIV and *REM instructions have
			 * side effects (unless we can prove that the second argument is
			 * non-zero). However, treating them as having side effects reduces
			 * the number of dummy variables we can remove, so we pretend they
			 * don't have any side effects.
			 *
			 * This doesn't seem to cause any problems with the client, as it
			 * doesn't deliberately try to trigger divide-by-zero exceptions.
			 */
		case Opcodes.INEG:
		case Opcodes.LNEG:
		case Opcodes.FNEG:
		case Opcodes.DNEG:
		case Opcodes.ISHL:
		case Opcodes.LSHL:
		case Opcodes.ISHR:
		case Opcodes.LSHR:
		case Opcodes.IUSHR:
		case Opcodes.LUSHR:
		case Opcodes.IAND:
		case Opcodes.LAND:
		case Opcodes.IOR:
		case Opcodes.LOR:
		case Opcodes.IXOR:
		case Opcodes.LXOR:
			return false;
		case Opcodes.IINC:
			return true;
		case Opcodes.I2L:
		case Opcodes.I2F:
		case Opcodes.I2D:
		case Opcodes.L2I:
		case Opcodes.L2F:
		case Opcodes.L2D:
		case Opcodes.F2I:
		case Opcodes.F2L:
		case Opcodes.F2D:
		case Opcodes.D2I:
		case Opcodes.D2L:
		case Opcodes.D2F:
		case Opcodes.I2B:
		case Opcodes.I2C:
		case Opcodes.I2S:
		case Opcodes.LCMP:
		case Opcodes.FCMPL:
		case Opcodes.FCMPG:
		case Opcodes.DCMPL:
		case Opcodes.DCMPG:
			return false;
		case Opcodes.IFEQ:
		case Opcodes.IFNE:
		case Opcodes.IFLT:
		case Opcodes.IFGE:
		case Opcodes.IFGT:
		case Opcodes.IFLE:
		case Opcodes.IF_ICMPEQ:
		case Opcodes.IF_ICMPNE:
		case Opcodes.IF_ICMPLT:
		case Opcodes.IF_ICMPGE:
		case Opcodes.IF_ICMPGT:
		case Opcodes.IF_ICMPLE:
		case Opcodes.IF_ACMPEQ:
		case Opcodes.IF_ACMPNE:
		case Opcodes.GOTO:
		case Opcodes.JSR:
		case Opcodes.RET:
		case Opcodes.TABLESWITCH:
		case Opcodes.LOOKUPSWITCH:
		case Opcodes.IRETURN:
		case Opcodes.LRETURN:
		case Opcodes.FRETURN:
		case Opcodes.DRETURN:
		case Opcodes.ARETURN:
		case Opcodes.RETURN:
			return true;
		case Opcodes.GETSTATIC:
			return false;
		case Opcodes.PUTSTATIC:
		case Opcodes.GETFIELD:
			/* might throw NPE */
		case Opcodes.PUTFIELD:
			return true;
		case Opcodes.INVOKEVIRTUAL:
		case Opcodes.INVOKESPECIAL:
		case Opcodes.INVOKESTATIC:
		case Opcodes.INVOKEINTERFACE:
		case Opcodes.INVOKEDYNAMIC:
			return true;
		case Opcodes.NEW:
			return false;
		case Opcodes.NEWARRAY:
		case Opcodes.ANEWARRAY:
			/* arrays might be created with negative size */
		case Opcodes.ARRAYLENGTH:
			/* might throw NPE */
		case Opcodes.ATHROW:
		case Opcodes.CHECKCAST:
			return true;
		case Opcodes.INSTANCEOF:
			return false;
		case Opcodes.MONITORENTER:
		case Opcodes.MONITOREXIT:
		case Opcodes.MULTIANEWARRAY:
		case Opcodes.IFNULL:
		case Opcodes.IFNONNULL:
			return true;
		}

		throw new IllegalArgumentException();
	}

	private InsnNodeUtils() {
		/* empty */
	}
}
