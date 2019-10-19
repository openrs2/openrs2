package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.ObjectCreationExpr;
import com.github.javaparser.ast.expr.TypeExpr;
import dev.openrs2.deob.ast.util.NodeUtils;

public final class ValueOfTransformer extends Transformer {
	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, ObjectCreationExpr.class, expr -> {
			if (!expr.getType().isBoxedType()) {
				return;
			}

			expr.replace(new MethodCallExpr(new TypeExpr(expr.getType()), "valueOf", expr.getArguments()));
		});
	}
}
