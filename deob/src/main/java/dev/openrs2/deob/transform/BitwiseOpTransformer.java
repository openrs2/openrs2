package dev.openrs2.deob.transform;

import java.util.HashMap;
import java.util.Map;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.MethodNodeUtils;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class BitwiseOpTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(BitwiseOpTransformer.class);

	private static final InsnMatcher BITWISE_OP_MATCHER = InsnMatcher.compile("^ILOAD ILOAD (IXOR | IAND | IOR) IRETURN$");
	private static final String BITWISE_OP_DESC = "(II)I";

	private final Map<MemberRef, Integer> methodOps = new HashMap<>();
	private int inlinedOps;

	@Override
	protected void preTransform(ClassPath classPath) {
		methodOps.clear();
		inlinedOps = 0;

		for (var library : classPath.getLibraries()) {
			for (var clazz : library) {
				for (var method : clazz.methods) {
					if (!MethodNodeUtils.hasCode(method)) {
						continue;
					}

					if ((method.access & Opcodes.ACC_STATIC) == 0) {
						continue;
					}

					if (!method.desc.equals(BITWISE_OP_DESC)) {
						continue;
					}

					BITWISE_OP_MATCHER.match(method).findAny().ifPresent(match -> {
						var iload0 = (VarInsnNode) match.get(0);
						if (iload0.var != 0) {
							return;
						}

						var iload1 = (VarInsnNode) match.get(1);
						if (iload1.var != 1) {
							return;
						}

						var methodRef = new MemberRef(clazz, method);
						methodOps.put(methodRef, match.get(2).getOpcode());
					});
				}
			}
		}
	}

	@Override
	protected boolean transformClass(ClassPath classPath, Library library, ClassNode clazz) {
		clazz.methods.removeIf(m -> methodOps.containsKey(new MemberRef(clazz, m)));
		return false;
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			if (insn.getOpcode() != Opcodes.INVOKESTATIC) {
				continue;
			}

			var invokestatic = (MethodInsnNode) insn;
			var methodRef = new MemberRef(invokestatic);
			var opcode = methodOps.get(methodRef);
			if (opcode != null) {
				it.set(new InsnNode(opcode));
				inlinedOps++;
			}
		}

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Inlined {} bitwise ops and removed {} redundant methods", inlinedOps, methodOps.size());
	}
}
