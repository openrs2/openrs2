package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.Expression;
import com.github.javaparser.ast.expr.UnaryExpr;
import dev.openrs2.deob.ast.util.ExprUtils;
import dev.openrs2.deob.ast.util.NodeUtils;
import dev.openrs2.deob.ast.util.TypeUtils;

public final class AddSubTransformer extends Transformer {
	private static boolean isNegative(Expression expr) {
		if (expr.isUnaryExpr()) {
			return expr.asUnaryExpr().getOperator() == UnaryExpr.Operator.MINUS;
		} else if (expr.isIntegerLiteralExpr()) {
			return expr.asIntegerLiteralExpr().asInt() < 0;
		} else if (expr.isLongLiteralExpr()) {
			return expr.asLongLiteralExpr().asLong() < 0;
		} else {
			return false;
		}
	}

	@Override
	public void transform(CompilationUnit unit) {
		NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, BinaryExpr.class, expr -> {
			var op = expr.getOperator();
			var left = expr.getLeft();
			var right = expr.getRight();

			var type = expr.calculateResolvedType();
			if (op == BinaryExpr.Operator.PLUS && TypeUtils.isString(type)) {
				return;
			}

			if (op == BinaryExpr.Operator.PLUS && isNegative(right)) {
				/* x + -y => x - y */
				expr.setOperator(BinaryExpr.Operator.MINUS);
				expr.setRight(ExprUtils.negate(right));
			} else if (op == BinaryExpr.Operator.PLUS && isNegative(left)) {
				/* -x + y => y - x */
				// TODO(gpe): check for side effects before applying this transform
			} else if (op == BinaryExpr.Operator.MINUS && isNegative(right)) {
				/* x - -y => x + y */
				expr.setOperator(BinaryExpr.Operator.PLUS);
				expr.setRight(ExprUtils.negate(right));
			}
		});
	}
}
