package dev.openrs2.asm;

import dev.openrs2.asm.classpath.ClassPath;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.AnalyzerException;

public abstract class Transformer {
	public void transform(ClassPath classPath) throws AnalyzerException {
		preTransform(classPath);

		for (var library : classPath.getLibraries()) {
			for (var clazz : library) {
				transformClass(clazz);

				for (var field : clazz.fields) {
					transformField(clazz, field);
				}

				for (var method : clazz.methods) {
					transformMethod(clazz, method);

					if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0) {
						transformCode(clazz, method);
					}
				}
			}
		}

		postTransform(classPath);
	}

	public void preTransform(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}

	public void transformClass(ClassNode clazz) throws AnalyzerException {
		/* empty */
	}

	public void transformField(ClassNode clazz, FieldNode field) throws AnalyzerException {
		/* empty */
	}

	public void transformMethod(ClassNode clazz, MethodNode method) throws AnalyzerException {
		/* empty */
	}

	public void transformCode(ClassNode clazz, MethodNode method) throws AnalyzerException {
		/* empty */
	}

	public void postTransform(ClassPath classPath) throws AnalyzerException {
		/* empty */
	}
}
