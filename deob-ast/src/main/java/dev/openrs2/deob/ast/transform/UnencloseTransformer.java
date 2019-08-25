package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.EnclosedExpr;
import dev.openrs2.deob.ast.util.NodeUtils;

public final class UnencloseTransformer extends Transformer {
	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, EnclosedExpr.class, expr -> {
			expr.replace(expr.getInner().clone());
		});
	}
}
