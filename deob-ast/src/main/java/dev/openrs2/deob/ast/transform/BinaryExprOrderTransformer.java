package dev.openrs2.deob.ast.transform;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;

public final class BinaryExprOrderTransformer extends Transformer {
	private static Optional<BinaryExpr.Operator> flip(BinaryExpr.Operator op) {
		// TODO(gpe): handle the PLUS operator (we can't flip it if the type of the expression is String)
		switch (op) {
		case MULTIPLY:
		case EQUALS:
		case NOT_EQUALS:
		case BINARY_AND:
		case BINARY_OR:
		case XOR:
		case OR:
		case AND:
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

	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(BinaryExpr.class).forEach(expr -> {
			flip(expr.getOperator()).ifPresent(op -> {
				var left = expr.getLeft();
				var right = expr.getRight();
				if (left.isLiteralExpr() && !right.isLiteralExpr()) {
					expr.setOperator(op);
					expr.setLeft(right);
					expr.setRight(left);
				}
			});
		});
	}
}
