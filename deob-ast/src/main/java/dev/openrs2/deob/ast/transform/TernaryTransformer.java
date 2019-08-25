package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.ConditionalExpr;
import dev.openrs2.deob.ast.util.ExprUtils;
import dev.openrs2.deob.ast.util.NodeUtils;

public final class TernaryTransformer extends Transformer {
	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, ConditionalExpr.class, expr -> {
			var condition = expr.getCondition();
			var notCondition = ExprUtils.not(condition);
			if (ExprUtils.countNots(notCondition) >= ExprUtils.countNots(condition)) {
				return;
			}

			var thenExpr = expr.getThenExpr();
			var elseExpr = expr.getElseExpr();

			expr.setCondition(notCondition);
			expr.setThenExpr(elseExpr.clone());
			expr.setElseExpr(thenExpr.clone());
		});
	}
}
