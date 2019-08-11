package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import dev.openrs2.deob.ast.util.ExprUtils;

public final class AddSubTransformer extends Transformer {
	private static Expression negate(Expression expr) {
		if (expr.isIntegerLiteralExpr()) {
			return new IntegerLiteralExpr(-expr.asIntegerLiteralExpr().asInt());
		} else if (expr.isLongLiteralExpr()) {
			return ExprUtils.createLong(-expr.asLongLiteralExpr().asLong());
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(UnaryExpr.class).forEach(expr -> {
			var operand = expr.getExpression();
			if (!ExprUtils.isIntegerOrLongLiteral(operand)) {
				return;
			}

			var op = expr.getOperator();
			if (op == UnaryExpr.Operator.PLUS) {
				expr.replace(operand);
			} else if (op == UnaryExpr.Operator.MINUS) {
				expr.replace(negate(operand));
			}
		});
	}
}
