package dev.openrs2.deob.transform;

import dev.openrs2.asm.classpath.ClassPath;
import dev.openrs2.asm.transform.Transformer;
import dev.openrs2.deob.remap.TypedRemapper;

public final class RemapTransformer extends Transformer {
	@Override
	protected void preTransform(ClassPath classPath) {
		classPath.remap(TypedRemapper.create(classPath));
	}
}
