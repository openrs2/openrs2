package dev.openrs2.bundler.transform;

import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RightClickTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(RightClickTransformer.class);

	private int metaDownCalls;

	@Override
	protected void preTransform(ClassPath classPath) {
		metaDownCalls = 0;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			if (insn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			var invokevirtual = (MethodInsnNode) insn;
			if (!invokevirtual.owner.equals("java/awt/event/MouseEvent")) {
				continue;
			}

			if (!invokevirtual.name.equals("isMetaDown")) {
				continue;
			}

			if (!invokevirtual.desc.equals("()Z")) {
				continue;
			}

			invokevirtual.name = "getModifiersEx";
			invokevirtual.desc = "()I";

			var list = new InsnList();
			list.add(new FieldInsnNode(Opcodes.GETSTATIC, "java/awt/event/MouseEvent", "BUTTON3_DOWN_MASK", "I"));
			list.add(new InsnNode(Opcodes.IAND));
			method.instructions.insert(invokevirtual, list);

			metaDownCalls++;
		}

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Replaced {} isMetaDown calls with getModifiersEx", metaDownCalls);
	}
}
