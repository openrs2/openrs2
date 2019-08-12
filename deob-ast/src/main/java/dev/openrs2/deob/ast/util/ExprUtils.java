package dev.openrs2.deob.ast.util;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.LongLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;

public final class ExprUtils {
	public static boolean isIntegerOrLongLiteral(Expression expr) {
		return expr.isIntegerLiteralExpr() || expr.isLongLiteralExpr();
	}

	public static LongLiteralExpr createLong(long value) {
		return new LongLiteralExpr(Long.toString(value).concat("L"));
	}

	public static Expression negate(Expression expr) {
		if (expr.isUnaryExpr() && expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS) {
			return expr.asUnaryExpr().getExpression().clone();
		} else if (expr.isIntegerLiteralExpr()) {
			return new IntegerLiteralExpr(-expr.asIntegerLiteralExpr().asInt());
		} else if (expr.isLongLiteralExpr()) {
			return createLong(-expr.asLongLiteralExpr().asLong());
		} else {
			throw new IllegalArgumentException();
		}
	}

	private ExprUtils() {
		/* empty */
	}
}
