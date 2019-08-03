package dev.openrs2.deob.transform;

import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

public final class CanvasTransformer extends Transformer {
	@Override
	public void transformClass(ClassNode clazz) {
		if (!"java/awt/Canvas".equals(clazz.superName)) {
			return;
		}

		if ((clazz.access & Opcodes.ACC_FINAL) == 0) {
			return;
		}

		clazz.interfaces.remove("java/awt/event/FocusListener");
	}
}
