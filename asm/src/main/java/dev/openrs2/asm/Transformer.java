package dev.openrs2.asm;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

public abstract class Transformer {
	public final void transform(Library library) {
		preTransform(library);

		for (var clazz : library) {
			transformClass(clazz);

			for (var method : clazz.methods) {
				if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0) {
					transformMethod(clazz, method);
				}
			}
		}

		postTransform(library);
	}

	public void preTransform(Library library) {
		/* empty */
	}

	public void transformClass(ClassNode clazz) {
		/* empty */
	}

	public void transformMethod(ClassNode clazz, MethodNode method) {
		/* empty */
	}

	public void postTransform(Library library) {
		/* empty */
	}
}
