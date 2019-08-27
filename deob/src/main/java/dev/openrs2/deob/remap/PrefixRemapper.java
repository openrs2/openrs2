package dev.openrs2.deob.remap;

import java.util.HashMap;

import dev.openrs2.asm.classpath.Library;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.commons.SimpleRemapper;

public final class PrefixRemapper {
	public static Remapper create(Library library, String prefix) {
		var mapping = new HashMap<String, String>();
		for (var clazz : library) {
			if (TypedRemapper.EXCLUDED_CLASSES.contains(clazz.name)) {
				mapping.put(clazz.name, clazz.name);
			} else {
				mapping.put(clazz.name, prefix + clazz.name);
			}
		}

		return new SimpleRemapper(mapping);
	}

	private PrefixRemapper() {
		/* empty */
	}
}
