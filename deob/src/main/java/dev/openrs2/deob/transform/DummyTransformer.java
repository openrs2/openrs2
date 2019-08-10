package dev.openrs2.deob.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import dev.openrs2.asm.InsnNodeUtils;
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
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DummyTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(DummyTransformer.class);

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

	private final Multimap<ArgRef, IntValue> argValues = HashMultimap.create();
	private final Map<DisjointSet.Partition<MemberRef>, Integer[]> constArgs = new HashMap<>();
	private DisjointSet<MemberRef> inheritedMethodSets;
	private int loadsInlined, branchesSimplified;

	@Override
	protected void preTransform(ClassPath classPath) {
		inheritedMethodSets = classPath.createInheritedMethodSets();
		loadsInlined = 0;
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
			case Opcodes.ILOAD:
				var iload = (VarInsnNode) insn;
				var value = frame.getLocal(iload.var);
				if (value.isConstant()) {
					method.instructions.set(insn, InsnNodeUtils.createIntConstant(value.getIntValue()));
					loadsInlined++;
					changed = true;
				}
				break;
			case Opcodes.IFEQ:
			case Opcodes.IFNE:
				value = frame.getStack(stackSize - 1);
				if (!value.isConstant()) {
					continue;
				}

				var taken = evaluateUnaryBranch(insn.getOpcode(), value.getIntValue());
				if (taken) {
					alwaysTakenUnaryBranches.add((JumpInsnNode) insn);
				} else {
					neverTakenUnaryBranches.add((JumpInsnNode) insn);
				}
				break;
			case Opcodes.IF_ICMPEQ:
			case Opcodes.IF_ICMPNE:
			case Opcodes.IF_ICMPLT:
			case Opcodes.IF_ICMPGE:
			case Opcodes.IF_ICMPGT:
			case Opcodes.IF_ICMPLE:
				var value1 = frame.getStack(stackSize - 2);
				if (!value1.isConstant()) {
					continue;
				}

				var value2 = frame.getStack(stackSize - 1);
				if (!value2.isConstant()) {
					continue;
				}

				taken = evaluateBinaryBranch(insn.getOpcode(), value1.getIntValue(), value2.getIntValue());
				if (taken) {
					alwaysTakenBinaryBranches.add((JumpInsnNode) insn);
				} else {
					neverTakenBinaryBranches.add((JumpInsnNode) insn);
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

			var parameters = new Integer[args];
			for (int i = 0; i < args; i++) {
				var values = argValues.get(new ArgRef(method, i));
				if (values.size() != 1) {
					continue;
				}

				var value = values.iterator().next();
				if (value.isConstant()) {
					parameters[i] = value.getIntValue();
				}
			}
			constArgs.put(method, parameters);
		}
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Inlined {} dummy loads and simplified {} dummy branches", loadsInlined, branchesSimplified);
	}
}
