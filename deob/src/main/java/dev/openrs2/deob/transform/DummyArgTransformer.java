package dev.openrs2.deob.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import dev.openrs2.asm.InsnListUtilsKt;
import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.InsnNodeUtilsKt;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.StackMetadataKt;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.common.collect.DisjointSet;
import dev.openrs2.deob.ArgRef;
import dev.openrs2.deob.analysis.IntInterpreter;
import dev.openrs2.deob.analysis.SourcedIntValue;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyArgTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(DummyArgTransformer.class);

	private static final InsnMatcher CONDITIONAL_CALL_MATCHER = InsnMatcher.compile("ILOAD (IFEQ | IFNE | (ICONST | BIPUSH | SIPUSH | LDC) (IF_ICMPEQ | IF_ICMPNE | IF_ICMPLT | IF_ICMPGE | IF_ICMPGT | IF_ICMPLE)) ALOAD? (ICONST | FCONST | DCONST | BIPUSH | SIPUSH | LDC | ACONST_NULL CHECKCAST)+ (INVOKEVIRTUAL | INVOKESTATIC | INVOKEINTERFACE)");

	private static final class ConditionalCall {
		private final int conditionVar, conditionOpcode;
		private final Integer conditionValue;
		private final DisjointSet.Partition<MemberRef> method;
		private final Integer[] constArgs;

		public ConditionalCall(int conditionVar, int conditionOpcode, Integer conditionValue, DisjointSet.Partition<MemberRef> method, Integer[] constArgs) {
			this.conditionVar = conditionVar;
			this.conditionOpcode = conditionOpcode;
			this.conditionValue = conditionValue;
			this.method = method;
			this.constArgs = constArgs;
		}

		@Override
		public String toString() {
			return MoreObjects.toStringHelper(this)
				.add("conditionVar", conditionVar)
				.add("conditionOpcode", conditionOpcode)
				.add("conditionValue", conditionValue)
				.add("method", method)
				.add("constArgs", constArgs)
				.toString();
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

	private final Multimap<ArgRef, SourcedIntValue> argValues = HashMultimap.create();
	private final Multimap<DisjointSet.Partition<MemberRef>, ConditionalCall> conditionalCalls = HashMultimap.create();
	private final Map<DisjointSet.Partition<MemberRef>, ImmutableSet<Integer>[]> constArgs = new HashMap<>();
	private DisjointSet<MemberRef> inheritedMethodSets;
	private int branchesSimplified, constantsInlined;

	private boolean isMutuallyRecursiveDummy(DisjointSet.Partition<MemberRef> method, int arg, DisjointSet.Partition<MemberRef> source, int value) {
		for (var sourceToMethodCall : conditionalCalls.get(source)) {
			if (!sourceToMethodCall.method.equals(method)) {
				continue;
			}

			for (var methodToSourceCall : conditionalCalls.get(method)) {
				if (!methodToSourceCall.method.equals(source)) {
					continue;
				}

				if (methodToSourceCall.conditionVar != arg) {
					continue;
				}

				boolean taken;
				if (methodToSourceCall.conditionValue != null) {
					taken = evaluateBinaryBranch(methodToSourceCall.conditionOpcode, value, methodToSourceCall.conditionValue);
				} else {
					taken = evaluateUnaryBranch(methodToSourceCall.conditionOpcode, value);
				}

				if (taken) {
					continue;
				}

				if (sourceToMethodCall.conditionValue != null) {
					taken = evaluateBinaryBranch(sourceToMethodCall.conditionOpcode, methodToSourceCall.constArgs[sourceToMethodCall.conditionVar], sourceToMethodCall.conditionValue);
				} else {
					taken = evaluateUnaryBranch(sourceToMethodCall.conditionOpcode, methodToSourceCall.constArgs[sourceToMethodCall.conditionVar]);
				}

				if (taken) {
					continue;
				}

				return true;
			}
		}

		return false;
	}

	private ImmutableSet<Integer> union(DisjointSet.Partition<MemberRef> method, int arg, Collection<SourcedIntValue> intValues) {
		var builder = ImmutableSet.<Integer>builder();

		for (var value : intValues) {
			var intValue = value.getIntValue();
			if (intValue.isUnknown()) {
				return null;
			}

			var source = value.getSource();
			if (source.equals(method)) {
				continue;
			}

			if (intValue.isSingleConstant()) {
				if (isMutuallyRecursiveDummy(method, arg, source, intValue.getIntValue())) {
					continue;
				}
			}

			builder.addAll(intValue.getIntValues());
		}

		var set = builder.build();
		if (set.isEmpty()) {
			return null;
		}

		return set;
	}

	@Override
	protected void preTransform(ClassPath classPath) {
		inheritedMethodSets = classPath.createInheritedMethodSets();
		branchesSimplified = 0;
		constantsInlined = 0;
	}

	@Override
	protected void prePass(ClassPath classPath) {
		argValues.clear();
		conditionalCalls.clear();
	}

	@Override
	protected boolean transformCode(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) throws AnalyzerException {
		var parentMethod = inheritedMethodSets.get(new MemberRef(clazz, method));

		var stores = new boolean[method.maxLocals];

		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();

			var opcode = insn.getOpcode();
			if (opcode != Opcodes.ISTORE) {
				continue;
			}

			var store = (VarInsnNode) insn;
			stores[store.var] = true;
		}

		CONDITIONAL_CALL_MATCHER.match(method).forEach(match -> {
			var matchIndex = 0;
			var load = (VarInsnNode) match.get(matchIndex++);
			if (stores[load.var]) {
				return;
			}

			var callerSlots = Type.getArgumentsAndReturnSizes(method.desc) >> 2;
			if ((method.access & Opcodes.ACC_STATIC) != 0) {
				callerSlots++;
			}

			if (load.var >= callerSlots) {
				return;
			}

			Integer conditionValue;
			var conditionOpcode = match.get(matchIndex).getOpcode();
			if (conditionOpcode == Opcodes.IFEQ || conditionOpcode == Opcodes.IFNE) {
				conditionValue = null;
				matchIndex++;
			} else {
				conditionValue = InsnNodeUtilsKt.getIntConstant(match.get(matchIndex++));
				conditionOpcode = match.get(matchIndex++).getOpcode();
			}

			var invoke = (MethodInsnNode) match.get(match.size() - 1);

			var invokeArgTypes = Type.getArgumentTypes(invoke.desc).length;
			var constArgs = new Integer[invokeArgTypes];

			if (invoke.getOpcode() != Opcodes.INVOKESTATIC) {
				matchIndex++;
			}

			for (int i = 0; i < constArgs.length; i++) {
				var insn = match.get(matchIndex++);
				if (insn.getOpcode() == Opcodes.ACONST_NULL) {
					matchIndex++;
				} else {
					constArgs[i] = InsnNodeUtilsKt.getIntConstant(insn);
				}
			}

			var callee = inheritedMethodSets.get(new MemberRef(invoke));
			if (callee == null) {
				return;
			}
			conditionalCalls.put(parentMethod, new ConditionalCall(load.var, conditionOpcode, conditionValue, callee, constArgs));
		});

		var parameters = constArgs.get(parentMethod);

		var analyzer = new Analyzer<>(new IntInterpreter(parameters));
		var frames = analyzer.analyze(clazz.name, method);

		var changed = false;

		var alwaysTakenBranches = new ArrayList<JumpInsnNode>();
		var neverTakenBranches = new ArrayList<JumpInsnNode>();
		var constInsns = new HashMap<AbstractInsnNode, Integer>();

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
				var invokedMethod = inheritedMethodSets.get(new MemberRef(invoke));
				if (invokedMethod == null) {
					continue;
				}

				var args = Type.getArgumentTypes(invoke.desc).length;
				for (int j = 0, k = 0; j < args; j++) {
					var arg = frame.getStack(stackSize - args + j);
					argValues.put(new ArgRef(invokedMethod, k), new SourcedIntValue(parentMethod, arg));
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
					alwaysTakenBranches.add((JumpInsnNode) insn);
					break;
				case NEVER_TAKEN:
					neverTakenBranches.add((JumpInsnNode) insn);
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
					alwaysTakenBranches.add((JumpInsnNode) insn);
					break;
				case NEVER_TAKEN:
					neverTakenBranches.add((JumpInsnNode) insn);
					break;
				}
				break;
			default:
				if (!InsnNodeUtilsKt.getPure(insn) || InsnNodeUtilsKt.getIntConstant(insn) != null) {
					continue;
				}

				if (StackMetadataKt.stackMetadata(insn).getPushes() != 1) {
					continue;
				}

				var nextInsn = InsnNodeUtilsKt.getNextReal(insn);
				if (nextInsn == null) {
					continue;
				}

				var nextInsnIndex = method.instructions.indexOf(nextInsn);
				var nextFrame = frames[nextInsnIndex];

				value = nextFrame.getStack(nextFrame.getStackSize() - 1);
				if (!value.isSingleConstant()) {
					continue;
				}

				constInsns.put(insn, value.getIntValue());
				break;
			}
		}

		for (var insn : alwaysTakenBranches) {
			if (InsnListUtilsKt.replaceSimpleExpression(method.instructions, insn, new JumpInsnNode(Opcodes.GOTO, insn.label))) {
				branchesSimplified++;
				changed = true;
			}
		}

		for (var insn : neverTakenBranches) {
			if (InsnListUtilsKt.deleteSimpleExpression(method.instructions, insn)) {
				branchesSimplified++;
				changed = true;
			}
		}

		for (var entry : constInsns.entrySet()) {
			var insn = entry.getKey();
			if (!method.instructions.contains(insn)) {
				continue;
			}

			var replacement = InsnNodeUtilsKt.createIntConstant(entry.getValue());
			if (InsnListUtilsKt.replaceSimpleExpression(method.instructions, insn, replacement)) {
				constantsInlined++;
				changed = true;
			}
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
				var parameter = union(method, i, argValues.get(new ArgRef(method, i)));
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
		logger.info("Simplified {} dummy branches and inlined {} constants", branchesSimplified, constantsInlined);
	}
}
