package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;

public abstract class Transformer {
	public abstract void transform(CompilationUnit unit);
}
