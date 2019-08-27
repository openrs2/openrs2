package dev.openrs2.deob.remap;

import java.util.HashMap;

import dev.openrs2.asm.classpath.Library;
import dev.openrs2.deob.transform.ClassForNameTransformer;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;

public final class ClassNamePrefixer {
	public static void addPrefix(Library library, String prefix) {
		var mapping = new HashMap<String, String>();
		for (var clazz : library) {
			if (TypedRemapper.EXCLUDED_CLASSES.contains(clazz.name)) {
				mapping.put(clazz.name, clazz.name);
			} else {
				mapping.put(clazz.name, prefix + clazz.name);
			}
		}
		var remapper = new SimpleRemapper(mapping);

		var transformer = new ClassForNameTransformer(remapper);
		for (var clazz : library) {
			for (var method : clazz.methods) {
				if ((method.access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) == 0) {
					transformer.transformCode(clazz, method);
				}
			}
		}

		for (var name : mapping.keySet()) {
			var in = library.remove(name);

			var out = new ClassNode();
			in.accept(new ClassRemapper(out, remapper));

			library.add(out);
		}
	}

	private ClassNamePrefixer() {
		/* empty */
	}
}
