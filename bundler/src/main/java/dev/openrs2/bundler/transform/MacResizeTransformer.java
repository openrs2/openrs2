package dev.openrs2.bundler.transform;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MacResizeTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(MacResizeTransformer.class);

	private static final InsnMatcher DETECT_MAC_MATCHER = InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL (IFEQ | IFNE)");

	private int branchesRemoved;

	@Override
	protected void preTransform(ClassPath classPath) {
		branchesRemoved = 0;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		DETECT_MAC_MATCHER.match(method).forEach(match -> {
			var getstatic = (FieldInsnNode) match.get(0);
			if (getstatic.owner.equals("loader") || getstatic.owner.equals(clazz.name) || !getstatic.desc.equals("Ljava/lang/String;")) {
				return;
			}

			var ldc = (LdcInsnNode) match.get(1);
			if (!ldc.cst.equals("mac")) {
				return;
			}

			var invokevirtual = (MethodInsnNode) match.get(2);
			if (!invokevirtual.owner.equals("java/lang/String") || !invokevirtual.name.equals("startsWith") || !invokevirtual.desc.equals("(Ljava/lang/String;)Z")) {
				return;
			}

			method.instructions.remove(getstatic);
			method.instructions.remove(ldc);
			method.instructions.remove(invokevirtual);

			var branch = (JumpInsnNode) match.get(3);
			if (branch.getOpcode() == Opcodes.IFEQ) {
				branch.setOpcode(Opcodes.GOTO);
			} else {
				method.instructions.remove(branch);
			}

			branchesRemoved++;
		});

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Removed {} branches to macOS-specific resize logic", branchesRemoved);
	}
}
