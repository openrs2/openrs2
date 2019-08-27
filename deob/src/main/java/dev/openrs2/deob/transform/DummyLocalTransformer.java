package dev.openrs2.deob.transform;

import com.google.common.collect.ImmutableSet;
import dev.openrs2.asm.InsnNodeUtils;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyLocalTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(DummyLocalTransformer.class);

	private static final ImmutableSet<Integer> LOAD_OPCODES = ImmutableSet.of(
		Opcodes.ILOAD,
		Opcodes.LLOAD,
		Opcodes.FLOAD,
		Opcodes.DLOAD,
		Opcodes.ALOAD
	);

	private static final ImmutableSet<Integer> STORE_OPCODES = ImmutableSet.of(
		Opcodes.ISTORE,
		Opcodes.LSTORE,
		Opcodes.FSTORE,
		Opcodes.DSTORE,
		Opcodes.ASTORE
	);

	private int localsRemoved;

	@Override
	protected void preTransform(ClassPath classPath) throws AnalyzerException {
		localsRemoved = 0;
	}

	@Override
	protected boolean transformCode(ClassNode clazz, MethodNode method) throws AnalyzerException {
		/*
		 * XXX(gpe): this is primitive (ideally we'd do a proper data flow
		 * analysis, but we'd need to do it in reverse and ASM only supports
		 * forward data flow), however, it seems to be good enough to catch
		 * everything.
		 */
		var loads = new boolean[method.maxLocals];

		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			if (LOAD_OPCODES.contains(insn.getOpcode())) {
				var load = (VarInsnNode) insn;
				loads[load.var] = true;
			}
		}

		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			var opcode = insn.getOpcode();
			if (!STORE_OPCODES.contains(opcode)) {
				continue;
			}

			var store = (VarInsnNode) insn;
			if (loads[store.var]) {
				continue;
			}

			if (InsnNodeUtils.deleteSimpleExpression(method.instructions, insn)) {
				localsRemoved++;
			}
		}

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) throws AnalyzerException {
		logger.info("Removed {} dummy local variables", localsRemoved);
	}
}
