package dev.openrs2.bundler.transform;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LoadLibraryTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(LoadLibraryTransformer.class);

	private static final InsnMatcher AMD64_CHECK_MATCHER = InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL IFNE GETSTATIC LDC INVOKEVIRTUAL IFNE");

	private int jnilibs, amd64Checks;

	@Override
	protected void preTransform(ClassPath classPath) {
		jnilibs = 0;
		amd64Checks = 0;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		var foundJnilib = false;

		for (var insn : method.instructions) {
			if (insn.getOpcode() == Opcodes.LDC) {
				var ldc = (LdcInsnNode) insn;
				if (ldc.cst.equals("libjaggl.jnilib")) {
					ldc.cst = "libjaggl.dylib";
					foundJnilib = true;
					jnilibs++;
					break;
				}
			}
		}

		if (!foundJnilib) {
			return false;
		}

		AMD64_CHECK_MATCHER.match(method).forEach(match -> {
			var ldc = (LdcInsnNode) match.get(1);
			if (ldc.cst.equals("amd64")) {
				match.forEach(method.instructions::remove);
				amd64Checks++;
			}
		});

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Replaced {} jnilibs with dylibs and removed {} amd64 jagmisc checks", jnilibs, amd64Checks);
	}
}
