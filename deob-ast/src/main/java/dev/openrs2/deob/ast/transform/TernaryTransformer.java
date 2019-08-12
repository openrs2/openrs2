package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.ConditionalExpr;
import dev.openrs2.deob.ast.util.ExprUtils;
import dev.openrs2.deob.ast.visitor.NegateExprVisitor;

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

			expr.setCondition(condition.accept(new NegateExprVisitor(), null));
			expr.setThenExpr(elseExpr.clone());
			expr.setElseExpr(thenExpr.clone());
		});
	}
}
