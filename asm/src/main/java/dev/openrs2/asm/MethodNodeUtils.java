package dev.openrs2.asm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicInterpreter;

public final class MethodNodeUtils {
	private static int localIndex(int access, List<Type> argTypes, int argIndex) {
		var localIndex = 0;
		if ((access & Opcodes.ACC_STATIC) == 0) {
			localIndex++;
		}
		for (int i = 0; i < argIndex; i++) {
			localIndex += argTypes.get(i).getSize();
		}
		return localIndex;
	}

	private static int remap(int i, Type argType, int localIndex) {
		if (i >= localIndex) {
			return i - argType.getSize();
		}
		return i;
	}

	private static List<Integer> remapAll(List<Integer> indexes, Type argType, int localIndex) {
		return indexes.stream()
			.map(i -> remap(i, argType, localIndex))
			.collect(Collectors.toList());
	}

	public static void deleteArgument(MethodNode method, int argIndex) {
		/* remove argument from the signature */
		var type = Type.getType(method.desc);

		var argTypes = new ArrayList<>(Arrays.asList(type.getArgumentTypes()));
		var argType = argTypes.remove(argIndex);

		method.desc = Type.getMethodDescriptor(type.getReturnType(), argTypes.toArray(Type[]::new));

		if (method.signature != null) {
			throw new UnsupportedOperationException("Signatures unsupported");
		}

		/* calculate index of the local we're removing */
		var localIndex = localIndex(method.access, argTypes, argIndex);

		/* remove ParameterNode */
		if (method.parameters != null) {
			method.parameters.remove(argIndex);
		}

		/* remove annotations */
		if (method.visibleAnnotableParameterCount != 0) {
			throw new UnsupportedOperationException("Non-zero visibleAnnotableParameterCount unsupported");
		}

		if (method.visibleParameterAnnotations != null) {
			@SuppressWarnings("unchecked")
			var annotations = (List<AnnotationNode>[]) new List<?>[method.visibleParameterAnnotations.length - 1];
			System.arraycopy(method.visibleParameterAnnotations, 0, annotations, 0, argIndex);
			System.arraycopy(method.visibleParameterAnnotations, argIndex + 1, annotations, argIndex, method.visibleParameterAnnotations.length - argIndex - 1);
			method.visibleParameterAnnotations = annotations;
		}

		if (method.invisibleAnnotableParameterCount != 0) {
			throw new UnsupportedOperationException("Non-zero invisibleAnnotableParameterCount unsupported");
		}

		if (method.invisibleParameterAnnotations != null) {
			@SuppressWarnings("unchecked")
			var annotations = (List<AnnotationNode>[]) new List<?>[method.invisibleParameterAnnotations.length - 1];
			System.arraycopy(method.invisibleParameterAnnotations, 0, annotations, 0, argIndex);
			System.arraycopy(method.invisibleParameterAnnotations, argIndex + 1, annotations, argIndex, method.invisibleParameterAnnotations.length - argIndex - 1);
			method.invisibleParameterAnnotations = annotations;
		}

		/* reduce maxLocals */
		method.maxLocals -= argType.getSize();

		/* remap locals */
		if (method.localVariables != null) {
			method.localVariables.removeIf(v -> v.index == localIndex);
			method.localVariables.forEach(v -> v.index = remap(v.index, argType, localIndex));
		}

		if (method.visibleLocalVariableAnnotations != null) {
			method.visibleLocalVariableAnnotations.removeIf(v -> v.index.contains(localIndex));
			method.visibleLocalVariableAnnotations.forEach(v -> v.index = remapAll(v.index, argType, localIndex));
		}

		if (method.invisibleLocalVariableAnnotations != null) {
			method.invisibleLocalVariableAnnotations.removeIf(v -> v.index.contains(localIndex));
			method.invisibleLocalVariableAnnotations.forEach(v -> v.index = remapAll(v.index, argType, localIndex));
		}

		for (var it = method.instructions.iterator(); it.hasNext(); ) {
			var insn = it.next();
			switch (insn.getType()) {
			case AbstractInsnNode.VAR_INSN:
				var varInsn = (VarInsnNode) insn;
				varInsn.var = remap(varInsn.var, argType, localIndex);
				break;
			case AbstractInsnNode.IINC_INSN:
				var iincInsn = (IincInsnNode) insn;
				iincInsn.var = remap(iincInsn.var, argType, localIndex);
				break;
			case AbstractInsnNode.FRAME:
				var frame = (FrameNode) insn;
				if (frame.type != Opcodes.F_NEW) {
					throw new UnsupportedOperationException("Only F_NEW frames are supported");
				}

				for (int i = 0; i < argType.getSize(); i++) {
					frame.local.remove(localIndex);
				}
				break;
			}
		}
	}

	public static void removeDeadCode(String owner, MethodNode method) throws AnalyzerException {
		boolean changed;
		do {
			changed = false;

			var analyzer = new Analyzer<>(new BasicInterpreter());
			var frames = analyzer.analyze(owner, method);

			var deadLabels = new HashSet<LabelNode>();
			var i = 0;
			for (var it = method.instructions.iterator(); it.hasNext(); ) {
				var insn = it.next();
				if (frames[i++] != null) {
					continue;
				}

				if (insn.getType() == AbstractInsnNode.LABEL) {
					deadLabels.add((LabelNode) insn);
				} else {
					it.remove();
					changed = true;
				}
			}

			if (method.tryCatchBlocks.removeIf(tryCatch -> deadLabels.contains(tryCatch.start) && deadLabels.contains(tryCatch.end))) {
				changed = true;
			}
		} while (changed);
	}

	private MethodNodeUtils() {
		/* empty */
	}
}
