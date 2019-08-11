package dev.openrs2.deob.ast.util;

import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.LongLiteralExpr;

public final class ExprUtils {
	public static boolean isIntegerOrLongLiteral(Expression expr) {
		return expr.isIntegerLiteralExpr() || expr.isLongLiteralExpr();
	}

	public static LongLiteralExpr createLong(long value) {
		return new LongLiteralExpr(Long.toString(value).concat("L"));
	}

	private ExprUtils() {
		/* empty */
	}
}
