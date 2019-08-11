package dev.openrs2.deob.ast.visitor;

import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.BooleanLiteralExpr;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import com.github.javaparser.ast.visitor.GenericVisitorWithDefaults;
import com.google.common.base.Preconditions;

// TODO(gpe): need to be careful about operator precedence/EnclosedExpr
public final class NegateExprVisitor extends GenericVisitorWithDefaults<Expression, Void> {
	@Override
	public Expression defaultAction(Node n, Void arg) {
		Preconditions.checkArgument(n instanceof Expression);
		return new UnaryExpr((Expression) n, UnaryExpr.Operator.LOGICAL_COMPLEMENT);
	}

	@Override
	public Expression defaultAction(NodeList n, Void arg) {
		throw new IllegalArgumentException();
	}

	@Override
	public Expression visit(BinaryExpr n, Void arg) {
		var left = n.getLeft();
		var right = n.getRight();
		switch (n.getOperator()) {
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
			return new BinaryExpr(left.accept(this, arg), right.accept(this, arg), BinaryExpr.Operator.OR);
		case OR:
			return new BinaryExpr(left.accept(this, arg), right.accept(this, arg), BinaryExpr.Operator.AND);
		default:
			return defaultAction(n, arg);
		}
	}

	@Override
	public Expression visit(BooleanLiteralExpr n, Void arg) {
		return new BooleanLiteralExpr(!n.getValue());
	}

	@Override
	public Expression visit(EnclosedExpr n, Void arg) {
		return n.getInner().accept(this, arg);
	}

	@Override
	public Expression visit(UnaryExpr n, Void arg) {
		switch (n.getOperator()) {
		case LOGICAL_COMPLEMENT:
			return n.getExpression();
		default:
			return defaultAction(n, arg);
		}
	}
}
