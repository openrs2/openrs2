package dev.openrs2.deob.classpath;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Strings;
import dev.openrs2.asm.MemberDesc;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.util.StringUtils;
import dev.openrs2.util.collect.DisjointSet;
import dev.openrs2.util.collect.ForestDisjointSet;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypedRemapper extends Remapper {
	private static final Logger logger = LoggerFactory.getLogger(TypedRemapper.class);

	public static final Set<String> EXCLUDED_CLASSES = Set.of(
		"client",
		"jagex3/jagmisc/jagmisc",
		"loader",
		"unpack",
		"unpackclass"
	);
	public static final Set<String> EXCLUDED_METHODS = Set.of(
		"<clinit>",
		"<init>",
		"main",
		"providesignlink",
		"quit"
	);
	public static final Set<String> EXCLUDED_FIELDS = Set.of(
		"cache"
	);
	private static final int MAX_OBFUSCATED_NAME_LEN = 2;

	public static TypedRemapper create(ClassPath classPath) {
		var libraryClasses = classPath.getLibraryClasses();

		var inheritedFieldSets = createInheritedFieldSets(libraryClasses);
		var inheritedMethodSets = createInheritedMethodSets(libraryClasses);

		var classes = createClassMapping(libraryClasses);
		var fields = createFieldMapping(classPath, inheritedFieldSets, classes);
		var methods = createMethodMapping(classPath, inheritedMethodSets);

		verifyMapping(classes);
		verifyMemberMapping(fields);
		verifyMemberMapping(methods);

		return new TypedRemapper(classes, fields, methods);
	}

	private static void verifyMapping(Map<String, String> mapping) {
		for (var entry : mapping.entrySet()) {
			verifyMapping(entry.getKey(), entry.getValue());
		}
	}

	private static void verifyMemberMapping(Map<MemberRef, String> mapping) {
		for (var entry : mapping.entrySet()) {
			verifyMapping(entry.getKey().getName(), entry.getValue());
		}
	}

	private static void verifyMapping(String name, String mappedName) {
		name = name.replaceAll("^(?:loader|unpacker)_", "");
		if (name.length() > MAX_OBFUSCATED_NAME_LEN && !name.equals(mappedName)) {
			logger.warn("Remapping probably unobfuscated name {} to {}", name, mappedName);
		}
	}

	private static DisjointSet<MemberRef> createInheritedFieldSets(List<ClassMetadata> classes) {
		var disjointSet = new ForestDisjointSet<MemberRef>();
		var ancestorCache = new HashMap<ClassMetadata, Set<MemberDesc>>();

		for (var clazz : classes) {
			populateInheritedFieldSets(ancestorCache, disjointSet, clazz);
		}

		return disjointSet;
	}

	private static Set<MemberDesc> populateInheritedFieldSets(Map<ClassMetadata, Set<MemberDesc>> ancestorCache, DisjointSet<MemberRef> disjointSet, ClassMetadata clazz) {
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
			if (EXCLUDED_FIELDS.contains(field.getName())) {
				continue;
			}

			disjointSet.add(new MemberRef(clazz.getName(), field));
			ancestors.add(field);
		}

		ancestors = Collections.unmodifiableSet(ancestors);
		ancestorCache.put(clazz, ancestors);
		return ancestors;
	}

	private static DisjointSet<MemberRef> createInheritedMethodSets(List<ClassMetadata> classes) {
		var disjointSet = new ForestDisjointSet<MemberRef>();
		var ancestorCache = new HashMap<ClassMetadata, Set<MemberDesc>>();

		for (var clazz : classes) {
			populateInheritedMethodSets(ancestorCache, disjointSet, clazz);
		}

		return disjointSet;
	}

	private static Set<MemberDesc> populateInheritedMethodSets(Map<ClassMetadata, Set<MemberDesc>> ancestorCache, DisjointSet<MemberRef> disjointSet, ClassMetadata clazz) {
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
			if (EXCLUDED_METHODS.contains(method.getName())) {
				continue;
			}

			disjointSet.add(new MemberRef(clazz.getName(), method));
			ancestors.add(method);
		}

		ancestors = Collections.unmodifiableSet(ancestors);
		ancestorCache.put(clazz, ancestors);
		return ancestors;
	}

	private static String generateName(Map<String, Integer> prefixes, String prefix) {
		return prefix + prefixes.merge(prefix, 1, Integer::sum);
	}

	private static Map<String, String> createClassMapping(List<ClassMetadata> classes) {
		var mapping = new HashMap<String, String>();
		var prefixes = new HashMap<String, Integer>();

		for (var clazz : classes) {
			populateClassMapping(mapping, prefixes, clazz);
		}

		return mapping;
	}

	private static String populateClassMapping(Map<String, String> mapping, Map<String, Integer> prefixes, ClassMetadata clazz) {
		var name = clazz.getName();
		if (mapping.containsKey(name) || EXCLUDED_CLASSES.contains(name) || clazz.isDependency()) {
			return mapping.getOrDefault(name, name);
		}

		var mappedName = name.substring(0, name.lastIndexOf('/') + 1);

		var superClass = clazz.getSuperClass();
		if (superClass != null && !superClass.getName().equals("java/lang/Object")) {
			var superName = populateClassMapping(mapping, prefixes, superClass);
			superName = superName.substring(superName.lastIndexOf('/') + 1);

			mappedName += generateName(prefixes, superName + "_Sub");
		} else if (clazz.isInterface()) {
			mappedName += generateName(prefixes, "Interface");
		} else {
			mappedName += generateName(prefixes, "Class");
		}

		mapping.put(name, mappedName);
		return mappedName;
	}

	private static Map<MemberRef, String> createFieldMapping(ClassPath classPath, DisjointSet<MemberRef> disjointSet, Map<String, String> classMapping) {
		var mapping = new HashMap<MemberRef, String>();
		var prefixes = new HashMap<String, Integer>();

		for (var partition : disjointSet) {
			boolean skip = false;

			for (var field : partition) {
				var clazz = classPath.get(field.getOwner());

				if (clazz.isDependency()) {
					skip = true;
					break;
				}
			}

			if (skip) {
				continue;
			}

			var prefix = "";

			var type = Type.getType(partition.iterator().next().getDesc());
			if (type.getSort() == Type.ARRAY) {
				prefix = Strings.repeat("Array", type.getDimensions());
				type = type.getElementType();
			}

			switch (type.getSort()) {
			case Type.BOOLEAN:
			case Type.BYTE:
			case Type.CHAR:
			case Type.SHORT:
			case Type.INT:
			case Type.LONG:
			case Type.FLOAT:
			case Type.DOUBLE:
				prefix = type.getClassName() + prefix;
				break;
			case Type.OBJECT:
				var className = classMapping.getOrDefault(type.getInternalName(), type.getInternalName());
				className = className.substring(className.lastIndexOf('/') + 1);
				prefix = className + prefix;
				break;
			default:
				throw new IllegalArgumentException("Unknown field type " + type);
			}

			prefix = StringUtils.indefiniteArticle(prefix) + StringUtils.capitalize(prefix);

			var mappedName = generateName(prefixes, prefix);
			for (var field : partition) {
				mapping.put(field, mappedName);
			}
		}

		return mapping;
	}

	private static Map<MemberRef, String> createMethodMapping(ClassPath classPath, DisjointSet<MemberRef> disjointSet) {
		var mapping = new HashMap<MemberRef, String>();
		var id = 0;

		for (var partition : disjointSet) {
			boolean skip = false;

			for (var method : partition) {
				var clazz = classPath.get(method.getOwner());

				if (clazz.isDependency()) {
					skip = true;
					break;
				}

				if (clazz.isNative(new MemberDesc(method.getName(), method.getDesc()))) {
					skip = true;
					break;
				}
			}

			if (skip) {
				continue;
			}

			var mappedName = "method" + (++id);
			for (var method : partition) {
				mapping.put(method, mappedName);
			}
		}

		return mapping;
	}

	private final Map<String, String> classes;
	private final Map<MemberRef, String> fields, methods;

	private TypedRemapper(Map<String, String> classes, Map<MemberRef, String> fields, Map<MemberRef, String> methods) {
		this.classes = classes;
		this.fields = fields;
		this.methods = methods;
	}

	@Override
	public String map(String internalName) {
		return classes.getOrDefault(internalName, internalName);
	}

	@Override
	public String mapFieldName(String owner, String name, String descriptor) {
		return fields.getOrDefault(new MemberRef(owner, name, descriptor), name);
	}

	@Override
	public String mapMethodName(String owner, String name, String descriptor) {
		return methods.getOrDefault(new MemberRef(owner, name, descriptor), name);
	}
}
