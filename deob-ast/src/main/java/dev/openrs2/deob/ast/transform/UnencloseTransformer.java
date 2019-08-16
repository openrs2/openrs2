package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.EnclosedExpr;

public final class UnencloseTransformer extends Transformer {
	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(EnclosedExpr.class).forEach(expr -> {
			expr.replace(expr.getInner().clone());
		});
	}
}
