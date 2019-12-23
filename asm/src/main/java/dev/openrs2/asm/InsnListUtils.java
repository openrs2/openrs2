package dev.openrs2.asm;

import java.util.ArrayList;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

public final class InsnListUtils {
	public static boolean replaceSimpleExpression(InsnList list, AbstractInsnNode last, AbstractInsnNode replacement) {
		var deadInsns = new ArrayList<AbstractInsnNode>();

		var height = 0;
		var insn = last;
		do {
			var metadata = StackMetadataKt.stackMetadata(insn);
			if (insn != last) {
				deadInsns.add(insn);
				height -= metadata.getPushes();
			}
			height += metadata.getPops();

			if (height == 0) {
				deadInsns.forEach(list::remove);

				if (replacement != null) {
					list.set(last, replacement);
				} else {
					list.remove(last);
				}

				return true;
			}

			insn = insn.getPrevious();
		} while (insn != null && insn.getType() != AbstractInsnNode.LABEL && InsnNodeUtils.isPure(insn));

		return false;
	}

	public static boolean deleteSimpleExpression(InsnList list, AbstractInsnNode last) {
		return replaceSimpleExpression(list, last, null);
	}

	private InsnListUtils() {
		/* empty */
	}
}
