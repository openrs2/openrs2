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

	private InsnNodeUtils() {
		/* empty */
	}
}
