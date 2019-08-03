package dev.openrs2.deob;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import com.google.common.collect.Sets;
import dev.openrs2.asm.InsnMatcher;
import dev.openrs2.asm.classpath.Library;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SignedClassSet {
	private static final Logger logger = LoggerFactory.getLogger(SignedClassSet.class);

	private static final InsnMatcher LOAD_SIGNED_CLASS_MATCHER = InsnMatcher.compile("LDC INVOKESTATIC ASTORE ALOAD GETFIELD ALOAD INVOKEVIRTUAL ALOAD INVOKEVIRTUAL POP");

	public static SignedClassSet create(Library loader, Library client) {
		/* find signed classes */
		var signedClasses = findSignedClasses(loader);
		logger.info("Identified signed classes {}", signedClasses);

		var dependencies = findDependencies(loader, signedClasses);
		logger.info("Identified signed class dependencies {}", dependencies);

		/* rename dependencies of signed classes so they don't clash with client classes */
		var mapping = new HashMap<String, String>();
		for (var dependency : dependencies) {
			mapping.put(dependency, "loader_" + dependency);
		}
		var remapper = new SimpleRemapper(mapping);

		/* move signed classes to the client */
		var remappedSignedClasses = new HashSet<String>();
		for (var name : Sets.union(signedClasses, dependencies)) {
			var in = loader.remove(name);

			var out = new ClassNode();
			in.accept(new ClassRemapper(out, remapper));

			remappedSignedClasses.add(out.name);
			client.add(out);
		}
		return new SignedClassSet(remappedSignedClasses);
	}

	private static Set<String> findSignedClasses(Library loader) {
		var clazz = loader.get("loader");
		if (clazz == null) {
			throw new IllegalArgumentException("Failed to find loader class");
		}

		for (var method : clazz.methods) {
			if (method.name.equals("run") && method.desc.equals("()V")) {
				return findSignedClasses(method);
			}
		}

		throw new IllegalArgumentException("Failed to find loader.run() method");
	}

	private static Set<String> findSignedClasses(MethodNode method) {
		var classes = new HashSet<String>();

		LOAD_SIGNED_CLASS_MATCHER.match(method).forEach(match -> {
			var ldc = (LdcInsnNode) match.get(0);
			if (ldc.cst instanceof String && !ldc.cst.equals("unpack")) {
				classes.add((String) ldc.cst);
			}
		});

		return classes;
	}

	private static Set<String> findDependencies(Library loader, Set<String> signedClasses) {
		var dependencies = new HashSet<String>();

		for (var signedClass : signedClasses) {
			var clazz = loader.get(signedClass);

			for (var field : clazz.fields) {
				var type = Type.getType(field.desc);
				if (type.getSort() != Type.OBJECT) {
					continue;
				}

				var name = type.getClassName();
				if (loader.contains(name) && !signedClasses.contains(name)) {
					dependencies.add(name);
				}
			}
		}

		return dependencies;
	}

	private final Set<String> signedClasses;

	private SignedClassSet(Set<String> signedClasses) {
		this.signedClasses = signedClasses;
	}

	public void move(Library client, Library signLink) {
		for (var name : signedClasses) {
			signLink.add(client.remove(name));
		}
	}
}
