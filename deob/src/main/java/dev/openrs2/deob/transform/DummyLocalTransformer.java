package dev.openrs2.deob.transform;

import dev.openrs2.asm.InsnListUtils;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyLocalTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(DummyLocalTransformer.class);

	private int localsRemoved;

	@Override
	protected void preTransform(ClassPath classPath) {
		localsRemoved = 0;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		/*
		 * XXX(gpe): this is primitive (ideally we'd do a proper data flow
		 * analysis, but we'd need to do it in reverse and ASM only supports
		 * forward data flow), however, it seems to be good enough to catch
		 * most dummy locals.
		 */
		var loads = new boolean[method.maxLocals];

		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();

			var opcode = insn.getOpcode();
			if (opcode != Opcodes.ILOAD) {
				continue;
			}

			var load = (VarInsnNode) insn;
			loads[load.var] = true;
		}

		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();

			var opcode = insn.getOpcode();
			if (opcode != Opcodes.ISTORE) {
				continue;
			}

			var store = (VarInsnNode) insn;
			if (loads[store.var]) {
				continue;
			}

			if (InsnListUtils.deleteSimpleExpression(method.instructions, insn)) {
				localsRemoved++;
			}
		}

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Removed {} dummy local variables", localsRemoved);
	}
}
