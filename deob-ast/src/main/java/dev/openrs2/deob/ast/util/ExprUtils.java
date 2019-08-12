package dev.openrs2.deob.ast.util;

import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
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

	public static boolean isNot(Expression expr) {
		return expr.isUnaryExpr() && expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT;
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

	// TODO(gpe): need to be careful about operator precedence/EnclosedExpr
	public static Expression not(Expression expr) {
		if (expr.isUnaryExpr()) {
			var unary = expr.asUnaryExpr();
			if (unary.getOperator() == UnaryExpr.Operator.LOGICAL_COMPLEMENT) {
				return unary.getExpression().clone();
			}
		} else if (expr.isBinaryExpr()) {
			var binary = expr.asBinaryExpr();

			var left = binary.getLeft();
			var right = binary.getRight();

			switch (binary.getOperator()) {
			case EQUALS:
				return new BinaryExpr(left, right, BinaryExpr.Operator.NOT_EQUALS);
			case NOT_EQUALS:
				return new BinaryExpr(left, right, BinaryExpr.Operator.EQUALS);
			case GREATER:
				return new BinaryExpr(left, right, BinaryExpr.Operator.LESS_EQUALS);
			case GREATER_EQUALS:
				return new BinaryExpr(left, right, BinaryExpr.Operator.LESS);
			case LESS:
				return new BinaryExpr(left, right, BinaryExpr.Operator.GREATER_EQUALS);
			case LESS_EQUALS:
				return new BinaryExpr(left, right, BinaryExpr.Operator.GREATER);
			case AND:
				return new BinaryExpr(not(left), not(right), BinaryExpr.Operator.OR);
			case OR:
				return new BinaryExpr(not(left), not(right), BinaryExpr.Operator.AND);
			}
		} else if (expr.isBooleanLiteralExpr()) {
			return new BooleanLiteralExpr(!expr.asBooleanLiteralExpr().getValue());
		} else if (expr.isEnclosedExpr()) {
			return not(expr.asEnclosedExpr().getInner());
		}
		return new UnaryExpr(expr.clone(), UnaryExpr.Operator.LOGICAL_COMPLEMENT);
	}

	public static boolean hasSideEffects(Expression expr) {
		if (expr.isLiteralExpr() || expr.isNameExpr() | expr.isFieldAccessExpr()) {
			return false;
		} else if (expr.isEnclosedExpr()) {
			return hasSideEffects(expr.asEnclosedExpr().getInner());
		} else if (expr.isUnaryExpr()) {
			return hasSideEffects(expr.asUnaryExpr().getExpression());
		} else if (expr.isBinaryExpr()) {
			var binary = expr.asBinaryExpr();
			return hasSideEffects(binary.getLeft()) || hasSideEffects(binary.getRight());
		} else if (expr.isArrayAccessExpr()) {
			var access = expr.asArrayAccessExpr();
			return hasSideEffects(access.getName()) || hasSideEffects(access.getIndex());
		}
		// TODO(gpe): more cases
		return true;
	}

	private ExprUtils() {
		/* empty */
	}
}
