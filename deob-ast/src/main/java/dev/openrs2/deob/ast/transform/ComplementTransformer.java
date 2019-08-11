package dev.openrs2.deob.ast.transform;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.expr.UnaryExpr;
import dev.openrs2.deob.ast.util.ExprUtils;

public final class ComplementTransformer extends Transformer {
	private static boolean isComplement(Expression expr) {
		return expr.isUnaryExpr() && expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.BITWISE_COMPLEMENT;
	}

	private static boolean isComplementOrLiteral(Expression expr) {
		return isComplement(expr) || ExprUtils.isIntegerOrLongLiteral(expr);
	}

	private static Optional<BinaryExpr.Operator> complement(BinaryExpr.Operator op) {
		switch (op) {
		case EQUALS:
		case NOT_EQUALS:
			return Optional.of(op);
		case GREATER:
			return Optional.of(BinaryExpr.Operator.LESS);
		case GREATER_EQUALS:
			return Optional.of(BinaryExpr.Operator.LESS_EQUALS);
		case LESS:
			return Optional.of(BinaryExpr.Operator.GREATER);
		case LESS_EQUALS:
			return Optional.of(BinaryExpr.Operator.GREATER_EQUALS);
		default:
			return Optional.empty();
		}
	}

	private static Expression complement(Expression expr) {
		if (expr.isUnaryExpr()) {
			return expr.asUnaryExpr().getExpression();
		} else if (expr.isIntegerLiteralExpr()) {
			return new IntegerLiteralExpr(~expr.asIntegerLiteralExpr().asInt());
		} else if (expr.isLongLiteralExpr()) {
			return ExprUtils.createLong(~expr.asLongLiteralExpr().asLong());
		} else {
			throw new IllegalArgumentException();
		}
	}

	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(BinaryExpr.class).forEach(expr -> {
			complement(expr.getOperator()).ifPresent(op -> {
				var left = expr.getLeft();
				var right = expr.getRight();

				if (isComplementOrLiteral(left) && isComplementOrLiteral(right) && !(ExprUtils.isIntegerOrLongLiteral(left) && ExprUtils.isIntegerOrLongLiteral(right))) {
					expr.setOperator(op);
					expr.setLeft(complement(left));
					expr.setRight(complement(right));
				}
			});
		});
	}
}
