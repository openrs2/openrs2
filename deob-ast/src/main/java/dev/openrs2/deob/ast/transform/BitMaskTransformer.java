package dev.openrs2.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.BinaryExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.google.common.collect.ImmutableSet;
import dev.openrs2.deob.ast.util.ExprUtils;

public final class BitMaskTransformer extends Transformer {
	private static final ImmutableSet<BinaryExpr.Operator> SHIFT_OPS = ImmutableSet.of(
		BinaryExpr.Operator.SIGNED_RIGHT_SHIFT,
		BinaryExpr.Operator.UNSIGNED_RIGHT_SHIFT
	);

	private static final ImmutableSet<BinaryExpr.Operator> BITWISE_OPS = ImmutableSet.of(
		BinaryExpr.Operator.BINARY_AND,
		BinaryExpr.Operator.BINARY_OR,
		BinaryExpr.Operator.XOR
	);

	@Override
	public void transform(CompilationUnit unit) {
		unit.findAll(BinaryExpr.class).forEach(expr -> {
			var shiftOp = expr.getOperator();
			var left = expr.getLeft();
			var shamtExpr = expr.getRight();

			if (!SHIFT_OPS.contains(shiftOp) || !left.isBinaryExpr() || !shamtExpr.isIntegerLiteralExpr()) {
				return;
			}

			var bitwiseExpr = left.asBinaryExpr();
			var bitwiseOp = bitwiseExpr.getOperator();
			var argExpr = bitwiseExpr.getLeft();
			var maskExpr = bitwiseExpr.getRight();

			if (!BITWISE_OPS.contains(bitwiseOp) || !ExprUtils.isIntegerOrLongLiteral(maskExpr)) {
				return;
			}

			var shamt = shamtExpr.asIntegerLiteralExpr().asInt();
			if (maskExpr.isIntegerLiteralExpr()) {
				var mask = maskExpr.asIntegerLiteralExpr().asInt();

				switch (shiftOp) {
				case SIGNED_RIGHT_SHIFT:
					mask >>= shamt;
					break;
				case UNSIGNED_RIGHT_SHIFT:
					mask >>>= shamt;
					break;
				default:
					throw new IllegalStateException();
				}

				maskExpr = new IntegerLiteralExpr(mask);
			} else {
				var mask = maskExpr.asLongLiteralExpr().asLong();

				switch (shiftOp) {
				case SIGNED_RIGHT_SHIFT:
					mask >>= shamt;
					break;
				case UNSIGNED_RIGHT_SHIFT:
					mask >>>= shamt;
					break;
				default:
					throw new IllegalStateException();
				}

				maskExpr = ExprUtils.createLong(mask);
			}

			expr.replace(new BinaryExpr(new BinaryExpr(argExpr.clone(), shamtExpr.clone(), shiftOp), maskExpr, bitwiseOp));
		});
	}
}
