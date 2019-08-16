package dev.openrs2.asm.classpath;

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import dev.openrs2.asm.MemberDesc;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.util.collect.DisjointSet;
import dev.openrs2.util.collect.ForestDisjointSet;
import org.objectweb.asm.tree.ClassNode;

public final class ClassPath {
	private final ClassLoader runtime;
	private final ImmutableList<Library> dependencies, libraries;
	private final Map<String, ClassMetadata> cache = new HashMap<>();

	public ClassPath(ClassLoader runtime, ImmutableList<Library> dependencies, ImmutableList<Library> libraries) {
		this.runtime = runtime;
		this.dependencies = dependencies;
		this.libraries = libraries;
	}

	public ImmutableList<Library> getLibraries() {
		return libraries;
	}

	public ImmutableList<ClassMetadata> getLibraryClasses() {
		var classes = ImmutableList.<ClassMetadata>builder();

		for (var library : libraries) {
			for (var clazz : library) {
				classes.add(get(clazz.name));
			}
		}

		return classes.build();
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

	public ClassNode getNode(String name) {
		for (var library : libraries) {
			var clazz = library.get(name);
			if (clazz != null) {
				return clazz;
			}
		}
		return null;
	}

	public DisjointSet<MemberRef> createInheritedFieldSets() {
		var disjointSet = new ForestDisjointSet<MemberRef>();
		var ancestorCache = new HashMap<ClassMetadata, ImmutableSet<MemberDesc>>();

		for (var library : libraries) {
			for (var clazz : library) {
				populateInheritedFieldSets(ancestorCache, disjointSet, get(clazz.name));
			}
		}

		return disjointSet;
	}

	private ImmutableSet<MemberDesc> populateInheritedFieldSets(Map<ClassMetadata, ImmutableSet<MemberDesc>> ancestorCache, DisjointSet<MemberRef> disjointSet, ClassMetadata clazz) {
		var ancestors = ancestorCache.get(clazz);
		if (ancestors != null) {
			return ancestors;
		}
		var ancestorsBuilder = ImmutableSet.<MemberDesc>builder();

		var superClass = clazz.getSuperClass();
		if (superClass != null) {
			var fields = populateInheritedFieldSets(ancestorCache, disjointSet, superClass);

			for (var field : fields) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), field));
				var partition2 = disjointSet.add(new MemberRef(superClass.getName(), field));
				disjointSet.union(partition1, partition2);
			}

			ancestorsBuilder.addAll(fields);
		}

		for (var superInterface : clazz.getSuperInterfaces()) {
			var fields = populateInheritedFieldSets(ancestorCache, disjointSet, superInterface);

			for (var field : fields) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), field));
				var partition2 = disjointSet.add(new MemberRef(superInterface.getName(), field));
				disjointSet.union(partition1, partition2);
			}

			ancestorsBuilder.addAll(fields);
		}

		for (var field : clazz.getFields()) {
			disjointSet.add(new MemberRef(clazz.getName(), field));
			ancestorsBuilder.add(field);
		}

		ancestors = ancestorsBuilder.build();
		ancestorCache.put(clazz, ancestors);
		return ancestors;
	}

	public DisjointSet<MemberRef> createInheritedMethodSets() {
		var disjointSet = new ForestDisjointSet<MemberRef>();
		var ancestorCache = new HashMap<ClassMetadata, ImmutableSet<MemberDesc>>();

		for (var library : libraries) {
			for (var clazz : library) {
				populateInheritedMethodSets(ancestorCache, disjointSet, get(clazz.name));
			}
		}

		return disjointSet;
	}

	private ImmutableSet<MemberDesc> populateInheritedMethodSets(Map<ClassMetadata, ImmutableSet<MemberDesc>> ancestorCache, DisjointSet<MemberRef> disjointSet, ClassMetadata clazz) {
		var ancestors = ancestorCache.get(clazz);
		if (ancestors != null) {
			return ancestors;
		}
		var ancestorsBuilder = ImmutableSet.<MemberDesc>builder();

		var superClass = clazz.getSuperClass();
		if (superClass != null) {
			var methods = populateInheritedMethodSets(ancestorCache, disjointSet, superClass);

			for (var method : methods) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), method));
				var partition2 = disjointSet.add(new MemberRef(superClass.getName(), method));
				disjointSet.union(partition1, partition2);
			}

			ancestorsBuilder.addAll(methods);
		}

		for (var superInterface : clazz.getSuperInterfaces()) {
			var methods = populateInheritedMethodSets(ancestorCache, disjointSet, superInterface);

			for (var method : methods) {
				var partition1 = disjointSet.add(new MemberRef(clazz.getName(), method));
				var partition2 = disjointSet.add(new MemberRef(superInterface.getName(), method));
				disjointSet.union(partition1, partition2);
			}

			ancestorsBuilder.addAll(methods);
		}

		for (var method : clazz.getMethods()) {
			disjointSet.add(new MemberRef(clazz.getName(), method));
			ancestorsBuilder.add(method);
		}

		ancestors = ancestorsBuilder.build();
		ancestorCache.put(clazz, ancestors);
		return ancestors;
	}
}
