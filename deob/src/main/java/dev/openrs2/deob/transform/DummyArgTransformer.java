package dev.openrs2.deob.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.deob.analysis.IntInterpreter;
import dev.openrs2.deob.analysis.IntValue;
import dev.openrs2.util.collect.DisjointSet;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyArgTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(DummyArgTransformer.class);

	private static final class ArgRef {
		private final DisjointSet.Partition<MemberRef> method;
		private final int arg;

		public ArgRef(DisjointSet.Partition<MemberRef> method, int arg) {
			this.method = method;
			this.arg = arg;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}
			ArgRef argRef = (ArgRef) o;
			return arg == argRef.arg &&
				method.equals(argRef.method);
		}

		@Override
		public int hashCode() {
			return Objects.hash(method, arg);
		}
	}

	private enum BranchResult {
		ALWAYS_TAKEN,
		NEVER_TAKEN,
		UNKNOWN;

		public static BranchResult fromTakenNotTaken(int taken, int notTaken) {
			Preconditions.checkArgument(taken != 0 || notTaken != 0);

			if (taken == 0) {
				return NEVER_TAKEN;
			} else if (notTaken == 0) {
				return ALWAYS_TAKEN;
			} else {
				return UNKNOWN;
			}
		}
	}

	private static BranchResult evaluateUnaryBranch(int opcode, Set<Integer> values) {
		Preconditions.checkArgument(!values.isEmpty());

		int taken = 0, notTaken = 0;
		for (var v : values) {
			if (evaluateUnaryBranch(opcode, v)) {
				taken++;
			} else {
				notTaken++;
			}
		}

		return BranchResult.fromTakenNotTaken(taken, notTaken);
	}

	private static boolean evaluateUnaryBranch(int opcode, int value) {
		switch (opcode) {
		case Opcodes.IFEQ:
			return value == 0;
		case Opcodes.IFNE:
			return value != 0;
		default:
			throw new IllegalArgumentException();
		}
	}

	private static BranchResult evaluateBinaryBranch(int opcode, Set<Integer> values1, Set<Integer> values2) {
		Preconditions.checkArgument(!values1.isEmpty() && !values2.isEmpty());

		int taken = 0, notTaken = 0;
		for (var v1 : values1) {
			for (var v2 : values2) {
				if (evaluateBinaryBranch(opcode, v1, v2)) {
					taken++;
				} else {
					notTaken++;
				}
			}
		}

		return BranchResult.fromTakenNotTaken(taken, notTaken);
	}

	private static boolean evaluateBinaryBranch(int opcode, int value1, int value2) {
		switch (opcode) {
		case Opcodes.IF_ICMPEQ:
			return value1 == value2;
		case Opcodes.IF_ICMPNE:
			return value1 != value2;
		case Opcodes.IF_ICMPLT:
			return value1 < value2;
		case Opcodes.IF_ICMPGE:
			return value1 >= value2;
		case Opcodes.IF_ICMPGT:
			return value1 > value2;
		case Opcodes.IF_ICMPLE:
			return value1 <= value2;
		default:
			throw new IllegalArgumentException();
		}
	}

	private static ImmutableSet<Integer> union(Collection<IntValue> intValues) {
		var builder = ImmutableSet.<Integer>builder();

		for (var value : intValues) {
			if (value.isUnknown()) {
				return null;
			}

			builder.addAll(value.getIntValues());
		}

		var set = builder.build();
		if (set.isEmpty()) {
			return null;
		}

		return set;
	}

	private final Multimap<ArgRef, IntValue> argValues = HashMultimap.create();
	private final Map<DisjointSet.Partition<MemberRef>, ImmutableSet<Integer>[]> constArgs = new HashMap<>();
	private DisjointSet<MemberRef> inheritedMethodSets;
	private int branchesSimplified;

	@Override
	protected void preTransform(ClassPath classPath) {
		inheritedMethodSets = classPath.createInheritedMethodSets();
		branchesSimplified = 0;
	}

	@Override
	protected void prePass(ClassPath classPath) {
		argValues.clear();
	}

	@Override
	protected boolean transformCode(ClassNode clazz, MethodNode method) throws AnalyzerException {
		var parentMethod = inheritedMethodSets.get(new MemberRef(clazz.name, method.name, method.desc));
		var parameters = constArgs.get(parentMethod);

		var analyzer = new Analyzer<>(new IntInterpreter(parameters));
		analyzer.analyze(clazz.name, method);

		var frames = analyzer.getFrames();

		var changed = false;

		var alwaysTakenUnaryBranches = new ArrayList<JumpInsnNode>();
		var neverTakenUnaryBranches = new ArrayList<JumpInsnNode>();

		var alwaysTakenBinaryBranches = new ArrayList<JumpInsnNode>();
		var neverTakenBinaryBranches = new ArrayList<JumpInsnNode>();

		for (var i = 0; i < frames.length; i++) {
			var frame = frames[i];
			if (frame == null) {
				continue;
			}

			var stackSize = frame.getStackSize();

			var insn = method.instructions.get(i);
			switch (insn.getOpcode()) {
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				var invoke = (MethodInsnNode) insn;
				var invokedMethod = inheritedMethodSets.get(new MemberRef(invoke.owner, invoke.name, invoke.desc));
				if (invokedMethod == null) {
					continue;
				}

				var args = Type.getArgumentTypes(invoke.desc).length;
				for (int j = 0, k = 0; j < args; j++) {
					var arg = frame.getStack(stackSize - args + j);
					argValues.put(new ArgRef(invokedMethod, k), arg);
					k += arg.getSize();
				}
				break;
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
				var value = frame.getStack(stackSize - 1);
				if (value.isUnknown()) {
					continue;
				}

				var result = evaluateUnaryBranch(insn.getOpcode(), value.getIntValues());
				switch (result) {
				case ALWAYS_TAKEN:
					alwaysTakenUnaryBranches.add((JumpInsnNode) insn);
					break;
				case NEVER_TAKEN:
					neverTakenUnaryBranches.add((JumpInsnNode) insn);
					break;
				}
				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
				var value1 = frame.getStack(stackSize - 2);
				var value2 = frame.getStack(stackSize - 1);
				if (value1.isUnknown() || value2.isUnknown()) {
					continue;
				}

				result = evaluateBinaryBranch(insn.getOpcode(), value1.getIntValues(), value2.getIntValues());
				switch (result) {
				case ALWAYS_TAKEN:
					alwaysTakenBinaryBranches.add((JumpInsnNode) insn);
					break;
				case NEVER_TAKEN:
					neverTakenBinaryBranches.add((JumpInsnNode) insn);
					break;
				}
				break;
			}
		}

		for (var insn : alwaysTakenUnaryBranches) {
			method.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
			method.instructions.set(insn, new JumpInsnNode(Opcodes.GOTO, insn.label));
			branchesSimplified++;
			changed = true;
		}

		for (var insn : neverTakenUnaryBranches) {
			method.instructions.set(insn, new InsnNode(Opcodes.POP));
			branchesSimplified++;
			changed = true;
		}

		for (var insn : alwaysTakenBinaryBranches) {
			method.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
			method.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
			method.instructions.set(insn, new JumpInsnNode(Opcodes.GOTO, insn.label));
			branchesSimplified++;
			changed = true;
		}

		for (var insn : neverTakenBinaryBranches) {
			method.instructions.insertBefore(insn, new InsnNode(Opcodes.POP));
			method.instructions.set(insn, new InsnNode(Opcodes.POP));
			branchesSimplified++;
			changed = true;
		}

		return changed;
	}

	@Override
	protected void postPass(ClassPath classPath) {
		for (var method : inheritedMethodSets) {
			var args = (Type.getArgumentsAndReturnSizes(method.iterator().next().getDesc()) >> 2) - 1;

			var allUnknown = true;
			@SuppressWarnings("unchecked")
			var parameters = (ImmutableSet<Integer>[]) new ImmutableSet<?>[args];
			for (var i = 0; i < args; i++) {
				var parameter = union(argValues.get(new ArgRef(method, i)));
				if (parameter != null) {
					allUnknown = false;
				}
				parameters[i] = parameter;
			}

			if (allUnknown) {
				constArgs.remove(method);
			} else {
				constArgs.put(method, parameters);
			}
		}
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Simplified {} dummy branches", branchesSimplified);
	}
}
