package dev.openrs2.deob.transform;

import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AccessTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(AccessTransformer.class);

	private int redundantFinals;

	@Override
	protected void preTransform(ClassPath classPath) {
		redundantFinals = 0;
	}

	@Override
	protected boolean preTransformMethod(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		if ((method.access & Opcodes.ACC_FINAL) == 0) {
			return false;
		}

		if ((method.access & (Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC)) != 0) {
			method.access &= ~Opcodes.ACC_FINAL;
			redundantFinals++;
		}
		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Removed {} redundant final modifiers", redundantFinals);
	}
}
