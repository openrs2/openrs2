package dev.openrs2.deob.transform;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.Library;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OpaquePredicateTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(OpaquePredicateTransformer.class);

	private static final InsnMatcher FLOW_OBSTRUCTOR_INITIALIZER_MATCHER = InsnMatcher.compile("(GETSTATIC | ILOAD) IFEQ (((GETSTATIC ISTORE)? IINC ILOAD) | ((GETSTATIC | ILOAD) IFEQ ICONST GOTO ICONST)) PUTSTATIC");
	private static final InsnMatcher OPAQUE_PREDICATE_MATCHER = InsnMatcher.compile("(GETSTATIC | ILOAD) (IFEQ | IFNE)");
	private static final InsnMatcher STORE_MATCHER = InsnMatcher.compile("GETSTATIC ISTORE");

	private final Set<MemberRef> flowObstructors = new HashSet<>();
	private int opaquePredicates, stores;

	@Override
	public void preTransform(Library library) {
		flowObstructors.clear();
		opaquePredicates = 0;
		stores = 0;

		for (var clazz : library) {
			for (var method : clazz.methods) {
				if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0) {
					findFlowObstructors(library, method);
				}
			}
		}

		logger.info("Identified flow obstructors {}", flowObstructors);
	}

	private void findFlowObstructors(Library library, MethodNode method) {
		FLOW_OBSTRUCTOR_INITIALIZER_MATCHER.match(method).forEach(match -> {
			/* add flow obstructor to set */
			var putstatic = (FieldInsnNode) match.get(match.size() - 1);
			flowObstructors.add(new MemberRef(putstatic.owner, putstatic.name, putstatic.desc));

			/* remove initializer */
			match.forEach(method.instructions::remove);

			/* remove field */
			var owner = library.get(putstatic.owner);
			owner.fields.removeIf(field -> field.name.equals(putstatic.name) && field.desc.equals(putstatic.desc));
		});
	}

	private boolean isFlowObstructor(FieldInsnNode insn) {
		return flowObstructors.contains(new MemberRef(insn.owner, insn.name, insn.desc));
	}

	private boolean isOpaquePredicate(MethodNode method, List<AbstractInsnNode> match) {
		var load = match.get(0);

		/* flow obstructor loaded directly? */
		if (load.getOpcode() == Opcodes.GETSTATIC) {
			var getstatic = (FieldInsnNode) load;
			return isFlowObstructor(getstatic);
		}

		/* flow obstructor loaded via local variable? */
		var iload = (VarInsnNode) load;
		return STORE_MATCHER.match(method).anyMatch(storeMatch -> {
			var getstatic = (FieldInsnNode) storeMatch.get(0);
			var istore = (VarInsnNode) storeMatch.get(1);
			return isFlowObstructor(getstatic) && iload.var == istore.var;
		});
	}

	private boolean isRedundantStore(List<AbstractInsnNode> match) {
		var getstatic = (FieldInsnNode) match.get(0);
		return isFlowObstructor(getstatic);
	}

	@Override
	public void transformCode(ClassNode clazz, MethodNode method) {
		/* find and fix opaque predicates */
		OPAQUE_PREDICATE_MATCHER.match(method).filter(match -> isOpaquePredicate(method, match)).forEach(match -> {
			var branch = (JumpInsnNode) match.get(1);

			if (branch.getOpcode() == Opcodes.IFEQ) {
				/* branch is always taken */
				method.instructions.remove(match.get(0));
				branch.setOpcode(Opcodes.GOTO);
			} else { /* IFNE */
				/* branch is never taken */
				match.forEach(method.instructions::remove);
			}

			opaquePredicates++;
		});

		/* remove redundant stores */
		STORE_MATCHER.match(method).filter(this::isRedundantStore).forEach(match -> {
			match.forEach(method.instructions::remove);
			stores++;
		});
	}

	@Override
	public void postTransform(Library library) {
		logger.info("Removed {} opaque predicates and {} redundant stores", opaquePredicates, stores);
	}
}
