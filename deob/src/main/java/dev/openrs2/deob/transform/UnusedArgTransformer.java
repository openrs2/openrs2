package dev.openrs2.deob.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.MethodNodeUtilsKt;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.classpath.Library;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.deob.ArgRef;
import dev.openrs2.deob.analysis.ConstSourceInterpreter;
import dev.openrs2.deob.remap.TypedRemapper;
import dev.openrs2.util.collect.DisjointSet;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UnusedArgTransformer extends Transformer {
	private static final Logger logger = LoggerFactory.getLogger(UnusedArgTransformer.class);

	private static final ImmutableSet<Integer> INT_SORTS = ImmutableSet.of(Type.BOOLEAN, Type.BYTE, Type.SHORT, Type.INT, Type.CHAR);

	private static Map<Integer, Integer> createLocalToArgMap(MethodNode method) {
		var type = Type.getType(method.desc);
		var argumentTypes = type.getArgumentTypes();

		var map = new HashMap<Integer, Integer>();
		var argIndex = 0;
		var localIndex = 0;

		if ((method.access & Opcodes.ACC_STATIC) == 0) {
			localIndex++;
		}

		for (var t : argumentTypes) {
			map.put(localIndex, argIndex++);
			localIndex += t.getSize();
		}

		return map;
	}

	private final Set<ArgRef> retainedArgs = new HashSet<>();
	private DisjointSet<MemberRef> inheritedMethodSets;
	private int deletedArgs;

	@Override
	protected void preTransform(ClassPath classPath) throws AnalyzerException {
		retainedArgs.clear();
		inheritedMethodSets = classPath.createInheritedMethodSets();
		deletedArgs = 0;

		for (var library : classPath.getLibraries()) {
			for (var clazz : library) {
				for (var method : clazz.methods) {
					if (MethodNodeUtilsKt.hasCode(method)) {
						populateRetainedArgs(classPath, clazz, method);
					}
				}
			}
		}
	}

	private void populateRetainedArgs(ClassPath classPath, ClassNode clazz, MethodNode method) throws AnalyzerException {
		var partition = inheritedMethodSets.get(new MemberRef(clazz, method));
		var localToArgMap = createLocalToArgMap(method);

		var analyzer = new Analyzer<>(new ConstSourceInterpreter());
		var frames = analyzer.analyze(clazz.name, method);

		for (var i = 0; i < frames.length; i++) {
			var frame = frames[i];
			if (frame == null) {
				continue;
			}

			var stackSize = frame.getStackSize();

			var insn = method.instructions.get(i);
			switch (insn.getOpcode()) {
			case Opcodes.ILOAD:
				var iload = (VarInsnNode) insn;
				var arg = localToArgMap.get(iload.var);
				if (arg != null) {
					retainedArgs.add(new ArgRef(partition, arg));
				}
				break;
			case Opcodes.INVOKEVIRTUAL:
			case Opcodes.INVOKESPECIAL:
			case Opcodes.INVOKESTATIC:
			case Opcodes.INVOKEINTERFACE:
				var invoke = (MethodInsnNode) insn;
				var invokePartition = inheritedMethodSets.get(new MemberRef(invoke));
				if (invokePartition == null || TypedRemapper.isMethodImmutable(classPath, invokePartition)) {
					continue;
				}

				var args = Type.getArgumentTypes(invoke.desc).length;
				for (var j = 0; j < args; j++) {
					var source = frame.getStack(stackSize - args + j);
					if (!source.isSingleSourceConstant()) {
						retainedArgs.add(new ArgRef(invokePartition, j));
					}
				}
				break;
			}
		}
	}

	@Override
	protected boolean preTransformMethod(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) throws AnalyzerException {
		/* delete unused int args from call sites */
		if (MethodNodeUtilsKt.hasCode(method)) {
			var analyzer = new Analyzer<>(new ConstSourceInterpreter());
			var frames = analyzer.analyze(clazz.name, method);
			var deadInsns = new ArrayList<AbstractInsnNode>();

			for (var i = 0; i < frames.length; i++) {
				var frame = frames[i];
				if (frame == null) {
					continue;
				}

				var stackSize = frame.getStackSize();

				var insn = method.instructions.get(i);
				if (insn.getType() != AbstractInsnNode.METHOD_INSN) {
					continue;
				}

				var methodInsn = (MethodInsnNode) insn;
				var partition = inheritedMethodSets.get(new MemberRef(methodInsn));
				if (partition == null || TypedRemapper.isMethodImmutable(classPath, partition)) {
					continue;
				}

				var type = Type.getType(methodInsn.desc);
				var argTypes = type.getArgumentTypes();

				var newArgTypes = new ArrayList<Type>();

				for (var j = 0; j < argTypes.length; j++) {
					var argType = argTypes[j];
					if (INT_SORTS.contains(argType.getSort()) && !retainedArgs.contains(new ArgRef(partition, j))) {
						var source = frame.getStack(stackSize - argTypes.length + j).getSource();
						deadInsns.add(source);
					} else {
						newArgTypes.add(argType);
					}
				}

				methodInsn.desc = Type.getMethodDescriptor(type.getReturnType(), newArgTypes.toArray(Type[]::new));
			}

			deadInsns.forEach(method.instructions::remove);
		}

		return false;
	}

	@Override
	protected boolean postTransformMethod(ClassPath classPath, Library library, ClassNode clazz, MethodNode method) {
		/* delete unused int args from the method itself */
		var partition = inheritedMethodSets.get(new MemberRef(clazz, method));
		if (TypedRemapper.isMethodImmutable(classPath, partition)) {
			return false;
		}

		var type = Type.getType(method.desc);
		var argTypes = type.getArgumentTypes();

		for (var i = argTypes.length - 1; i >= 0; i--) {
			var argType = argTypes[i];
			if (INT_SORTS.contains(argType.getSort()) && !retainedArgs.contains(new ArgRef(partition, i))) {
				MethodNodeUtilsKt.removeArgument(method, i);
				deletedArgs++;
			}
		}

		return false;
	}

	@Override
	protected void postTransform(ClassPath classPath) {
		logger.info("Removed {} dummy arguments", deletedArgs);
	}
}
