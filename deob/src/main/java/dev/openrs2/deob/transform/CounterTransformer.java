package dev.openrs2.deob.transform;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CounterTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(CounterTransformer.class);

	private static final InsnMatcher RESET_PATTERN = InsnMatcher.compile("ICONST_0 PUTSTATIC");
	private static final InsnMatcher INCREMENT_PATTERN = InsnMatcher.compile("GETSTATIC ICONST_1 IADD PUTSTATIC");

	private final Set<MemberRef> counters = new HashSet<>();

	@Override
	public void preTransform(ClassPath classPath) {
		counters.clear();

		var references = new HashMap<MemberRef, Integer>();
		var resets = new HashMap<MemberRef, Integer>();
		var increments = new HashMap<MemberRef, Integer>();

		for (var library : classPath.getLibraries()) {
			for (var clazz : library) {
				for (var method : clazz.methods) {
					if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0) {
						findCounters(method, references, resets, increments);
					}
				}
			}
		}

		deleteCounters(classPath, references, resets, increments);
	}

	private void findCounters(MethodNode method, Map<MemberRef, Integer> references, Map<MemberRef, Integer> resets, Map<MemberRef, Integer> increments) {
		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			if (insn.getType() != AbstractInsnNode.FIELD_INSN) {
				continue;
			}

			var fieldInsn = (FieldInsnNode) insn;
			references.merge(new MemberRef(fieldInsn), 1, Integer::sum);
		}

		RESET_PATTERN.match(method).forEach(match -> {
			var putstatic = (FieldInsnNode) match.get(1);
			resets.merge(new MemberRef(putstatic), 1, Integer::sum);
		});

		INCREMENT_PATTERN.match(method).forEach(match -> {
			var getstatic = (FieldInsnNode) match.get(0);
			var putstatic = (FieldInsnNode) match.get(3);
			if (getstatic.owner.equals(putstatic.owner) && getstatic.name.equals(putstatic.name) && getstatic.desc.equals(putstatic.desc)) {
				increments.merge(new MemberRef(putstatic), 1, Integer::sum);
			}
		});
	}

	private void deleteCounters(ClassPath classPath, Map<MemberRef, Integer> references, Map<MemberRef, Integer> resets, Map<MemberRef, Integer> increments) {
		for (Map.Entry<MemberRef, Integer> entry : references.entrySet()) {
			var counter = entry.getKey();

			if (entry.getValue() != 3) { /* one for the reset, two for the increment */
				continue;
			}

			if (resets.getOrDefault(counter, 0) != 1) {
				continue;
			}

			if (increments.getOrDefault(counter, 0) != 1) {
				continue;
			}

			ClassNode owner = classPath.getNode(counter.getOwner());
			owner.fields.removeIf(f -> f.name.equals(counter.getName()) && f.desc.equals(counter.getDesc()));

			counters.add(counter);
		}
	}

	@Override
	public boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		RESET_PATTERN.match(method).forEach(match -> {
			var putstatic = (FieldInsnNode) match.get(1);
			if (counters.contains(new MemberRef(putstatic))) {
				match.forEach(method.instructions::remove);
			}
		});

		INCREMENT_PATTERN.match(method).forEach(match -> {
			var getstatic = (FieldInsnNode) match.get(0);
			var putstatic = (FieldInsnNode) match.get(3);

			if (getstatic.owner.equals(putstatic.owner) && getstatic.name.equals(putstatic.name) && getstatic.desc.equals(putstatic.desc) &&
				counters.contains(new MemberRef(putstatic))) {
				match.forEach(method.instructions::remove);
			}
		});

		return false;
	}

	@Override
	public void postTransform(ClassPath classPath) {
		logger.info("Removed {} counters", counters.size());
	}
}
