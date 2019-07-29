package dev.openrs2.deob;

import java.util.HashMap;

import dev.openrs2.asm.Library;
import dev.openrs2.deob.classpath.TypedRemapper;
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
