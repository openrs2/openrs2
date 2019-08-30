package dev.openrs2.bundler.transform;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HostCheckTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(HostCheckTransformer.class);

	private static final InsnMatcher GET_HOST_MATCHER = InsnMatcher.compile("INVOKEVIRTUAL INVOKEVIRTUAL INVOKEVIRTUAL");

	private int hostChecks;

	@Override
	protected void preTransform(ClassPath classPath) {
		hostChecks = 0;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		if (Type.getReturnType(method.desc).getSort() != Type.BOOLEAN) {
			return false;
		}

		GET_HOST_MATCHER.match(method).forEach(match -> {
			var insn1 = (MethodInsnNode) match.get(0);
			if (!insn1.owner.equals(clazz.name)) {
				return;
			}

			if (!insn1.name.equals("getDocumentBase")) {
				return;
			}

			if (!insn1.desc.equals("()Ljava/net/URL;")) {
				return;
			}

			var insn2 = (MethodInsnNode) match.get(1);
			if (!insn2.owner.equals("java/net/URL")) {
				return;
			}

			if (!insn2.name.equals("getHost")) {
				return;
			}

			if (!insn2.desc.equals("()Ljava/lang/String;")) {
				return;
			}

			var insn3 = (MethodInsnNode) match.get(2);
			if (!insn3.owner.equals("java/lang/String")) {
				return;
			}

			if (!insn3.name.equals("toLowerCase")) {
				return;
			}

			if (!insn3.desc.equals("()Ljava/lang/String;")) {
				return;
			}

			method.instructions.clear();
			method.tryCatchBlocks.clear();

			method.instructions.add(new InsnNode(Opcodes.ICONST_1));
			method.instructions.add(new InsnNode(Opcodes.IRETURN));

			hostChecks++;
		});

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Removed {} host checks", hostChecks);
	}
}
