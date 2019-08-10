package dev.openrs2.asm.transform;

import dev.openrs2.asm.classpath.ClassPath;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class Transformer {
	public void transform(ClassPath classPath) throws AnalyzerException {
		preTransform(classPath);

		boolean changed;
		do {
			changed = false;

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
		} while (changed);

		postTransform(classPath);
	}

	public void preTransform(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}

	public boolean transformClass(ClassNode clazz) throws AnalyzerException {
		return false;
	}

	public boolean transformField(ClassNode clazz, FieldNode field) throws AnalyzerException {
		return false;
	}

	public boolean transformMethod(ClassNode clazz, MethodNode method) throws AnalyzerException {
		return false;
	}

	public boolean transformCode(ClassNode clazz, MethodNode method) throws AnalyzerException {
		return false;
	}

	public void postTransform(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}
}
