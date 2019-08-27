package dev.openrs2.asm.remap;

import java.util.List;

import dev.openrs2.asm.InsnMatcher;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public final class ClassForNameRemapper {
	private static final InsnMatcher INVOKE_MATCHER = InsnMatcher.compile("LDC INVOKESTATIC");

	private static boolean isClassForName(List<AbstractInsnNode> match) {
		var ldc = (LdcInsnNode) match.get(0);
		if (!(ldc.cst instanceof String)) {
			return false;
		}

		var invokestatic = (MethodInsnNode) match.get(1);
		return invokestatic.owner.equals("java/lang/Class") &&
			invokestatic.name.equals("forName") &&
			invokestatic.desc.equals("(Ljava/lang/String;)Ljava/lang/Class;");
	}

	public static void remap(Remapper remapper, MethodNode method) {
		INVOKE_MATCHER.match(method).filter(ClassForNameRemapper::isClassForName).forEach(match -> {
			var ldc = (LdcInsnNode) match.get(0);
			var name = remapper.map((String) ldc.cst);
			if (name != null) {
				ldc.cst = name;
			}
		});
	}

	private ClassForNameRemapper() {
		/* empty */
	}
}
