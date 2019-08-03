package dev.openrs2.deob.classpath;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import dev.openrs2.asm.Library;
import dev.openrs2.asm.MemberDesc;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.util.collect.DisjointSet;
import dev.openrs2.util.collect.ForestDisjointSet;

public final class ClassPath {
	private final ClassLoader runtime;
	private final List<Library> dependencies, libraries;
	private final Map<String, ClassMetadata> cache = new HashMap<>();

	public ClassPath(ClassLoader runtime, List<Library> dependencies, List<Library> libraries) {
		this.runtime = runtime;
		this.dependencies = dependencies;
		this.libraries = libraries;
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
				metadata = new AsmClassMetadata(this, clazz, false);
				cache.put(name, metadata);
				return metadata;
			}
		}

		for (var library : dependencies) {
			var clazz = library.get(name);
			if (clazz != null) {
				metadata = new AsmClassMetadata(this, clazz, true);
				cache.put(name, metadata);
				return metadata;
			}
		}

		var reflectionName = name.replace('/', '.');

		Class<?> clazz;
		try {
			clazz = runtime.loadClass(reflectionName);
		} catch (ClassNotFoundException ex) {
			throw new IllegalArgumentException("Unknown class " + name);
		}

		metadata = new ReflectionClassMetadata(this, clazz);
		cache.put(name, metadata);
		return metadata;
	}

	public DisjointSet<MemberRef> createInheritedFieldSets() {
		var disjointSet = new ForestDisjointSet<MemberRef>();
		var ancestorCache = new HashMap<ClassMetadata, Set<MemberDesc>>();

		for (var library : libraries) {
			for (var clazz : library) {
				populateInheritedFieldSets(ancestorCache, disjointSet, get(clazz.name));
			}
		}

		return disjointSet;
	}

	private Set<MemberDesc> populateInheritedFieldSets(Map<ClassMetadata, Set<MemberDesc>> ancestorCache, DisjointSet<MemberRef> disjointSet, ClassMetadata clazz) {
		var ancestors = ancestorCache.get(clazz);
		if (ancestors != null) {
			return ancestors;
		}
		ancestors = new HashSet<>();

		var superClass = clazz.getSuperClass();
		if (superClass != null) {
			var fields = populateInheritedFieldSets(ancestorCache, disjointSet, superClass);

			for (var field : fields) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), field));
				var partition2 = disjointSet.add(new MemberRef(superClass.getName(), field));
				disjointSet.union(partition1, partition2);
			}

			ancestors.addAll(fields);
		}

		for (var superInterface : clazz.getSuperInterfaces()) {
			var fields = populateInheritedFieldSets(ancestorCache, disjointSet, superInterface);

			for (var field : fields) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), field));
				var partition2 = disjointSet.add(new MemberRef(superInterface.getName(), field));
				disjointSet.union(partition1, partition2);
			}

			ancestors.addAll(fields);
		}

		for (var field : clazz.getFields()) {
			disjointSet.add(new MemberRef(clazz.getName(), field));
			ancestors.add(field);
		}

		ancestors = Collections.unmodifiableSet(ancestors);
		ancestorCache.put(clazz, ancestors);
		return ancestors;
	}

	public DisjointSet<MemberRef> createInheritedMethodSets() {
		var disjointSet = new ForestDisjointSet<MemberRef>();
		var ancestorCache = new HashMap<ClassMetadata, Set<MemberDesc>>();

		for (var library : libraries) {
			for (var clazz : library) {
				populateInheritedMethodSets(ancestorCache, disjointSet, get(clazz.name));
			}
		}

		return disjointSet;
	}

	private Set<MemberDesc> populateInheritedMethodSets(Map<ClassMetadata, Set<MemberDesc>> ancestorCache, DisjointSet<MemberRef> disjointSet, ClassMetadata clazz) {
		var ancestors = ancestorCache.get(clazz);
		if (ancestors != null) {
			return ancestors;
		}
		ancestors = new HashSet<>();

		var superClass = clazz.getSuperClass();
		if (superClass != null) {
			var methods = populateInheritedMethodSets(ancestorCache, disjointSet, superClass);

			for (var method : methods) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), method));
				var partition2 = disjointSet.add(new MemberRef(superClass.getName(), method));
				disjointSet.union(partition1, partition2);
			}

			ancestors.addAll(methods);
		}

		for (var superInterface : clazz.getSuperInterfaces()) {
			var methods = populateInheritedMethodSets(ancestorCache, disjointSet, superInterface);

			for (var method : methods) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), method));
				var partition2 = disjointSet.add(new MemberRef(superInterface.getName(), method));
				disjointSet.union(partition1, partition2);
			}

			ancestors.addAll(methods);
		}

		for (var method : clazz.getMethods()) {
			disjointSet.add(new MemberRef(clazz.getName(), method));
			ancestors.add(method);
		}

		ancestors = Collections.unmodifiableSet(ancestors);
		ancestorCache.put(clazz, ancestors);
		return ancestors;
	}
}
