package dev.openrs2.asm;

import org.objectweb.asm.tree.AbstractInsnNode;

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

	private InsnNodeUtils() {
		/* empty */
	}
}
