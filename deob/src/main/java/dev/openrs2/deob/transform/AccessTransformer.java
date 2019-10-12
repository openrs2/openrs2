package dev.openrs2.deob.transform;

import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AccessTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(AccessTransformer.class);

	private int redundantFinals, packagePrivate;

	@Override
	protected void preTransform(ClassPath classPath) {
		redundantFinals = 0;
		packagePrivate = 0;
	}

	@Override
	protected boolean transformClass(ClassPath classPath, Library library, ClassNode clazz) {
		if ((clazz.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0) {
			clazz.access |= Opcodes.ACC_PUBLIC;
			packagePrivate++;
		}
		return false;
	}

	@Override
	protected boolean transformField(ClassPath classPath, Library library, ClassNode clazz, FieldNode field) {
		if ((field.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0) {
			field.access |= Opcodes.ACC_PUBLIC;
			packagePrivate++;
		}
		return false;
	}

	@Override
	protected boolean preTransformMethod(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		if ((method.access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE)) == 0) {
			method.access |= Opcodes.ACC_PUBLIC;
			packagePrivate++;
		}

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
		logger.info("Made {} package-private classes, fields and methods public", packagePrivate);
	}
}
