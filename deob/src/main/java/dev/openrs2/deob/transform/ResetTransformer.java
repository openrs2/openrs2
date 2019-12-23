package dev.openrs2.deob.transform;

import java.util.HashSet;
import java.util.Set;

import dev.openrs2.asm.InsnNodeUtilsKt;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.MethodNodeUtilsKt;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ResetTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(ResetTransformer.class);

	private static MemberRef findMasterReset(MethodNode method) {
		AbstractInsnNode shutdownLdc = null;

		for (var insn : method.instructions) {
			if (insn.getOpcode() != Opcodes.LDC) {
				continue;
			}

			var ldc = (LdcInsnNode) insn;
			if (ldc.cst.equals("Shutdown complete - clean:")) {
				shutdownLdc = ldc;
				break;
			}
		}

		for (var insn = shutdownLdc; insn != null; insn = insn.getPrevious()) {
			if (insn.getOpcode() != Opcodes.ALOAD) {
				continue;
			}

			var load = (VarInsnNode) insn;
			if (load.var != 0) {
				continue;
			}

			var nextInsn = InsnNodeUtilsKt.getNextReal(insn);
			if (nextInsn.getOpcode() != Opcodes.INVOKEVIRTUAL) {
				continue;
			}

			var invoke = (MethodInsnNode) nextInsn;
			if (!invoke.desc.equals("()V")) {
				continue;
			}

			return new MemberRef(invoke);
		}

		return null;
	}

	private static void findResetMethods(Set<MemberRef> resetMethods, ClassNode clazz, MethodNode method) throws AnalyzerException {
		MethodNodeUtilsKt.removeDeadCode(method, clazz.name);

		for (var insn : method.instructions) {
			if (insn.getOpcode() == -1 || insn.getOpcode() != Opcodes.INVOKESTATIC) {
				continue;
			}

			var invoke = (MethodInsnNode) insn;
			resetMethods.add(new MemberRef(invoke));
		}
	}

	private final Set<MemberRef> resetMethods = new HashSet<>();

	@Override
	protected void preTransform(ClassPath classPath) throws AnalyzerException {
		resetMethods.clear();

		for (var library : classPath.getLibraries()) {
			for (var clazz : library) {
				for (var method : clazz.methods) {
					if (!MethodNodeUtilsKt.hasCode(method)) {
						continue;
					}

					var masterReset = findMasterReset(method);
					if (masterReset == null) {
						continue;
					}

					logger.info("Identified master reset method: {}", masterReset);

					var resetClass = classPath.getNode("client");
					var resetMethod = resetClass.methods.stream()
						.filter(m -> m.name.equals(masterReset.getName()) && m.desc.equals(masterReset.getDesc()))
						.findAny()
						.orElseThrow();

					findResetMethods(resetMethods, resetClass, resetMethod);

					resetMethod.instructions.clear();
					resetMethod.tryCatchBlocks.clear();

					resetMethod.instructions.add(new InsnNode(Opcodes.RETURN));
				}
			}
		}
	}

	@Override
	protected boolean transformClass(ClassPath classPath, Library library, ClassNode clazz) {
		clazz.methods.removeIf(m -> resetMethods.contains(new MemberRef(clazz, m)));
		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Removed {} reset methods", resetMethods.size());
	}
}
