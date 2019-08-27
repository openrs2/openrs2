package dev.openrs2.deob.remap;

import java.util.HashMap;

import dev.openrs2.asm.classpath.Library;
import org.objectweb.asm.commons.SimpleRemapper;

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

		library.remap(new SimpleRemapper(mapping));
	}

	private ClassNamePrefixer() {
		/* empty */
	}
}
