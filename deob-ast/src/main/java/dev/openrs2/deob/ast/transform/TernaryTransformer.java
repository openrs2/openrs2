package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ConditionalExpr;
import dev.openrs2.deob.ast.util.ExprUtils;

public final class TernaryTransformer extends Transformer {
	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(ConditionalExpr.class).forEach(expr -> {
			var condition = expr.getCondition();
			if (!ExprUtils.isNot(condition)) {
				return;
			}

			var thenExpr = expr.getThenExpr();
			var elseExpr = expr.getElseExpr();

			expr.setCondition(ExprUtils.not(condition));
			expr.setThenExpr(elseExpr.clone());
			expr.setElseExpr(thenExpr.clone());
		});
	}
}
