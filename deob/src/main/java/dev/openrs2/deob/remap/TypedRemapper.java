package dev.openrs2.deob.remap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import dev.openrs2.asm.MemberDesc;
import dev.openrs2.asm.MemberRef;
import dev.openrs2.asm.classpath.ClassMetadata;
import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.util.StringUtilsKt;
import dev.openrs2.util.collect.DisjointSet;
import kotlin.text.StringsKt;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.Remapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class TypedRemapper extends Remapper {
	private static final Logger logger = LoggerFactory.getLogger(TypedRemapper.class);

	public static final ImmutableSet<String> EXCLUDED_CLASSES = ImmutableSet.of(
		"client",
		"jagex3/jagmisc/jagmisc",
		"loader",
		"unpack",
		"unpackclass"
	);
	private static final ImmutableSet<String> EXCLUDED_METHODS = ImmutableSet.of(
		"<clinit>",
		"<init>",
		"main",
		"providesignlink",
		"quit"
	);
	private static final ImmutableSet<String> EXCLUDED_FIELDS = ImmutableSet.of(
		"cache"
	);
	private static final int MAX_OBFUSCATED_NAME_LEN = 2;

	public static TypedRemapper create(ClassPath classPath) {
		var inheritedFieldSets = classPath.createInheritedFieldSets();
		var inheritedMethodSets = classPath.createInheritedMethodSets();

		var classes = createClassMapping(classPath.getLibraryClasses());
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
		if (mapping.containsKey(name) || EXCLUDED_CLASSES.contains(name) || clazz.getDependency()) {
			return mapping.getOrDefault(name, name);
		}

		var mappedName = name.substring(0, name.lastIndexOf('/') + 1);

		var superClass = clazz.getSuperClass();
		if (superClass != null && !superClass.getName().equals("java/lang/Object")) {
			var superName = populateClassMapping(mapping, prefixes, superClass);
			superName = superName.substring(superName.lastIndexOf('/') + 1);

			mappedName += generateName(prefixes, superName + "_Sub");
		} else if (clazz.getInterface()) {
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

				if (EXCLUDED_FIELDS.contains(field.getName())) {
					skip = true;
					break;
				}

				if (clazz.getDependency()) {
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

			prefix = StringUtilsKt.indefiniteArticle(prefix) + StringsKt.capitalize(prefix);

			var mappedName = generateName(prefixes, prefix);
			for (var field : partition) {
				mapping.put(field, mappedName);
			}
		}

		return mapping;
	}

	public static boolean isMethodImmutable(ClassPath classPath, DisjointSet.Partition<MemberRef> partition) {
		for (var method : partition) {
			var clazz = classPath.get(method.getOwner());

			if (EXCLUDED_METHODS.contains(method.getName())) {
				return true;
			}

			if (clazz.getDependency()) {
				return true;
			}

			if (clazz.isNative(new MemberDesc(method))) {
				return true;
			}
		}

		return false;
	}

	private static Map<MemberRef, String> createMethodMapping(ClassPath classPath, DisjointSet<MemberRef> disjointSet) {
		var mapping = new HashMap<MemberRef, String>();
		var id = 0;

		for (var partition : disjointSet) {
			if (isMethodImmutable(classPath, partition)) {
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
