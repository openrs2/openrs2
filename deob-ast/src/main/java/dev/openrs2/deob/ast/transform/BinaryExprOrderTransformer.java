package dev.openrs2.deob.ast.transform;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import dev.openrs2.deob.ast.util.NodeUtils;
import dev.openrs2.deob.ast.util.TypeUtils;

public final class BinaryExprOrderTransformer extends Transformer {
	private static Optional<BinaryExpr.Operator> flip(BinaryExpr.Operator op) {
		switch (op) {
		case PLUS:
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
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, BinaryExpr.class, expr -> {
			flip(expr.getOperator()).ifPresent(op -> {
				var type = expr.calculateResolvedType();
				if (op == BinaryExpr.Operator.PLUS && TypeUtils.isString(type)) {
					return;
				}

				var left = expr.getLeft();
				var right = expr.getRight();
				if (left.isLiteralExpr() && !right.isLiteralExpr()) {
					expr.setOperator(op);
					expr.setLeft(right.clone());
					expr.setRight(left.clone());
				}
			});
		});
	}
}
