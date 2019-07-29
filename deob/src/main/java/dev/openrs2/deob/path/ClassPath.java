package dev.openrs2.deob.path;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.openrs2.asm.Library;

public final class ClassPath {
	private final ClassLoader dependencyLoader;
	private final List<Library> libraries;
	private final Map<String, ClassMetadata> cache = new HashMap<>();

	public ClassPath(ClassLoader dependencyLoader, Library... libraries) {
		this.dependencyLoader = dependencyLoader;
		this.libraries = List.of(libraries);
	}

	public List<ClassMetadata> getLibraryClasses() {
		var classes = new ArrayList<ClassMetadata>();

		for (var library : libraries) {
			for (var clazz : library) {
				classes.add(get(clazz.name));
			}
		}

		return Collections.unmodifiableList(classes);
	}

	public ClassMetadata get(String name) {
		var metadata = cache.get(name);
		if (metadata != null) {
			return metadata;
		}

		for (var library : libraries) {
			var clazz = library.get(name);
			if (clazz != null) {
				metadata = new AsmClassMetadata(this, clazz);
				cache.put(name, metadata);
				return metadata;
			}
		}

		var reflectionName = name.replace('/', '.');

		Class<?> clazz;
		try {
			clazz = dependencyLoader.loadClass(reflectionName);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Unknown class " + name);
		}

		metadata = new ReflectionClassMetadata(this, clazz);
		cache.put(name, metadata);
		return metadata;
	}
}
