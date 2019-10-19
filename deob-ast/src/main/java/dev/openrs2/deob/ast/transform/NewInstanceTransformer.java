package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.MethodCallExpr;
import dev.openrs2.deob.ast.util.NodeUtils;

public final class NewInstanceTransformer extends Transformer {
	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, MethodCallExpr.class, expr -> {
			if (!expr.getNameAsString().equals("newInstance")) {
				return;
			}

			expr.getScope().ifPresent(scope -> {
				if (scope.isMethodCallExpr() && scope.asMethodCallExpr().getNameAsString().equals("getConstructor")) {
					return;
				}

				expr.setScope(new MethodCallExpr(scope.clone(), "getDeclaredConstructor"));
			});
		});
	}
}
