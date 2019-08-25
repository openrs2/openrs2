package dev.openrs2.deob.ast.transform;

import java.util.Optional;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.EnclosedExpr;
import com.github.javaparser.ast.expr.Expression;
import dev.openrs2.deob.ast.util.NodeUtils;

public final class EncloseTransformer extends Transformer {
	private enum Associativity {
		LEFT,
		RIGHT,
		NONE
	}

	private enum Op {
		ACCESS_PARENS(Associativity.LEFT),
		POSTFIX(Associativity.NONE),
		UNARY(Associativity.RIGHT),
		CAST_NEW(Associativity.RIGHT),
		MULTIPLICATIVE(Associativity.LEFT),
		ADDITIVE(Associativity.LEFT),
		SHIFT(Associativity.LEFT),
		RELATIONAL(Associativity.LEFT),
		EQUALITY(Associativity.NONE),
		BITWISE_AND(Associativity.LEFT),
		BITWISE_XOR(Associativity.LEFT),
		BITWISE_OR(Associativity.LEFT),
		LOGICAL_AND(Associativity.LEFT),
		LOGICAL_OR(Associativity.LEFT),
		TERNARY(Associativity.RIGHT),
		ASSIGNMENT(Associativity.RIGHT);

		public static Optional<Op> from(Expression expr) {
			if (expr.isArrayAccessExpr() || expr.isFieldAccessExpr() || expr.isMethodCallExpr() || expr.isEnclosedExpr()) {
				return Optional.of(ACCESS_PARENS);
			} else if (expr.isUnaryExpr()) {
				return Optional.of(expr.asUnaryExpr().getOperator().isPostfix() ? POSTFIX : UNARY);
			} else if (expr.isCastExpr() || expr.isObjectCreationExpr() || expr.isArrayCreationExpr()) {
				return Optional.of(CAST_NEW);
			} else if (expr.isBinaryExpr()) {
				switch (expr.asBinaryExpr().getOperator()) {
				case MULTIPLY:
				case DIVIDE:
				case REMAINDER:
					return Optional.of(MULTIPLICATIVE);
				case PLUS:
				case MINUS:
					return Optional.of(ADDITIVE);
				case LEFT_SHIFT:
				case SIGNED_RIGHT_SHIFT:
				case UNSIGNED_RIGHT_SHIFT:
					return Optional.of(SHIFT);
				case LESS:
				case LESS_EQUALS:
				case GREATER:
				case GREATER_EQUALS:
					return Optional.of(RELATIONAL);
				case EQUALS:
				case NOT_EQUALS:
					return Optional.of(EQUALITY);
				case BINARY_AND:
					return Optional.of(BITWISE_AND);
				case XOR:
					return Optional.of(BITWISE_XOR);
				case BINARY_OR:
					return Optional.of(BITWISE_OR);
				case AND:
					return Optional.of(LOGICAL_AND);
				case OR:
					return Optional.of(LOGICAL_OR);
				}
			} else if (expr.isInstanceOfExpr()) {
				return Optional.of(RELATIONAL);
			} else if (expr.isConditionalExpr()) {
				return Optional.of(TERNARY);
			} else if (expr.isAssignExpr()) {
				return Optional.of(ASSIGNMENT);
			}
			return Optional.empty();
		}

		private final Associativity associativity;

		Op(Associativity associativity) {
			this.associativity = associativity;
		}

		public Associativity getAssociativity() {
			return associativity;
		}

		public boolean isPrecedenceLess(Op other) {
			return ordinal() > other.ordinal();
		}

		public boolean isPrecedenceLessEqual(Op other) {
			return ordinal() >= other.ordinal();
		}
	}

	private static void encloseLeft(Expression parent, Expression child) {
		var parentOp = Op.from(parent).orElseThrow(IllegalArgumentException::new);
		Op.from(child).ifPresent(childOp -> {
			switch (parentOp.getAssociativity()) {
			case LEFT:
				if (childOp.isPrecedenceLess(parentOp)) {
					parent.replace(child, new EnclosedExpr(child.clone()));
				}
				break;
			case NONE:
			case RIGHT:
				if (childOp.isPrecedenceLessEqual(parentOp)) {
					parent.replace(child, new EnclosedExpr(child.clone()));
				}
				break;
			default:
				throw new IllegalArgumentException();
			}
		});
	}

	private static void encloseRight(Expression parent, Expression child) {
		var parentOp = Op.from(parent).orElseThrow(IllegalArgumentException::new);
		Op.from(child).ifPresent(childOp -> {
			switch (parentOp.getAssociativity()) {
			case NONE:
			case LEFT:
				if (childOp.isPrecedenceLessEqual(parentOp)) {
					parent.replace(child, new EnclosedExpr(child.clone()));
				}
				break;
			case RIGHT:
				if (childOp.isPrecedenceLess(parentOp)) {
					parent.replace(child, new EnclosedExpr(child.clone()));
				}
				break;
			default:
				throw new IllegalArgumentException();
			}
		});
	}

	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, Expression.class, expr -> {
			if (expr.isArrayAccessExpr()) {
				var accessExpr = expr.asArrayAccessExpr();
				encloseLeft(expr, accessExpr.getName());
			} else if (expr.isFieldAccessExpr()) {
				encloseLeft(expr, expr.asFieldAccessExpr().getScope());
			} else if (expr.isMethodCallExpr()) {
				expr.asMethodCallExpr().getScope().ifPresent(scope -> {
					encloseLeft(expr, scope);
				});
			} else if (expr.isUnaryExpr()) {
				var unaryExpr = expr.asUnaryExpr();
				encloseRight(expr, unaryExpr.getExpression());
			} else if (expr.isCastExpr()) {
				encloseRight(expr, expr.asCastExpr().getExpression());
			} else if (expr.isObjectCreationExpr()) {
				expr.asObjectCreationExpr().getScope().ifPresent(scope -> {
					encloseLeft(expr, scope);
				});
			} else if (expr.isBinaryExpr()) {
				var binaryExpr = expr.asBinaryExpr();
				encloseLeft(expr, binaryExpr.getLeft());
				encloseRight(expr, binaryExpr.getRight());
			} else if (expr.isInstanceOfExpr()) {
				encloseLeft(expr, expr.asInstanceOfExpr().getExpression());
			} else if (expr.isConditionalExpr()) {
				var conditionalExpr = expr.asConditionalExpr();
				encloseLeft(expr, conditionalExpr.getCondition());
				encloseLeft(expr, conditionalExpr.getThenExpr());
				encloseRight(expr, conditionalExpr.getElseExpr());
			} else if (expr.isAssignExpr()) {
				var assignExpr = expr.asAssignExpr();
				encloseLeft(expr, assignExpr.getTarget());
				encloseRight(expr, assignExpr.getValue());
			}
		});
	}
}
