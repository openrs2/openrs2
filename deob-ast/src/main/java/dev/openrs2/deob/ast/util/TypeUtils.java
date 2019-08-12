package dev.openrs2.deob.ast.util;

import com.github.javaparser.resolution.types.ResolvedType;

public final class TypeUtils {
	public static boolean isString(ResolvedType type) {
		return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals("java.lang.String");
	}

	private TypeUtils() {
		/* empty */
	}
}
