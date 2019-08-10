package dev.openrs2.asm.transform;

import dev.openrs2.asm.classpath.ClassPath;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class Transformer {
	public final void transform(ClassPath classPath) throws AnalyzerException {
		preTransform(classPath);

		boolean changed;
		do {
			changed = false;

			prePass(classPath);
			for (var library : classPath.getLibraries()) {
				for (var clazz : library) {
					changed |= transformClass(clazz);

					for (var field : clazz.fields) {
						changed |= transformField(clazz, field);
					}

					for (var method : clazz.methods) {
						changed |= transformMethod(clazz, method);

						if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0) {
							changed |= transformCode(clazz, method);
						}
					}
				}
			}
			postPass(classPath);
		} while (changed);

		postTransform(classPath);
	}

	protected void preTransform(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}

	protected void prePass(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}

	protected boolean transformClass(ClassNode clazz) throws AnalyzerException {
		return false;
	}

	protected boolean transformField(ClassNode clazz, FieldNode field) throws AnalyzerException {
		return false;
	}

	protected boolean transformMethod(ClassNode clazz, MethodNode method) throws AnalyzerException {
		return false;
	}

	protected boolean transformCode(ClassNode clazz, MethodNode method) throws AnalyzerException {
		return false;
	}

	protected void postPass(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}

	protected void postTransform(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}
}
