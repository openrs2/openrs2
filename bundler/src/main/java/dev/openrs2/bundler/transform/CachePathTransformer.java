package dev.openrs2.bundler.transform;

import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CachePathTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(CachePathTransformer.class);

	private int paths;

	@Override
	protected void preTransform(ClassPath classPath) {
		paths = 0;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			if (insn.getOpcode() != Opcodes.LDC) {
				continue;
			}

			var ldc = (LdcInsnNode) insn;
			if (ldc.cst.equals(".jagex_cache_") || ldc.cst.equals(".file_store_")) {
				ldc.cst = ".openrs2_cache_";
				paths++;
			} else if (ldc.cst.equals("jagex_")) {
				ldc.cst = ".openrs2_";
				paths++;
			}
		}

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Updated {} cache paths", paths);
	}
}
