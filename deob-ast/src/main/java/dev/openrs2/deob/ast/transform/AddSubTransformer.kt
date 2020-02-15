package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.hasSideEffects
import dev.openrs2.deob.ast.util.isString
import dev.openrs2.deob.ast.util.negate
import dev.openrs2.deob.ast.util.walk

class AddSubTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val op = expr.operator
            val left = expr.left
            val right = expr.right
            val type = expr.calculateResolvedType()

            if (op == BinaryExpr.Operator.PLUS && type.isString()) {
                return@walk
            }

            if (op == BinaryExpr.Operator.PLUS && right.isNegative()) {
                // x + -y => x - y
                expr.operator = BinaryExpr.Operator.MINUS
                expr.right = right.negate()
            } else if (op == BinaryExpr.Operator.PLUS && left.isNegative()) {
                if (expr.hasSideEffects()) {
                    return@walk
                }

                // -x + y => y - x
                expr.operator = BinaryExpr.Operator.MINUS
                expr.left = right.clone()
                expr.right = left.negate()
            } else if (op == BinaryExpr.Operator.MINUS && right.isNegative()) {
                // x - -y => x + y
                expr.operator = BinaryExpr.Operator.PLUS
                expr.right = right.negate()
            }
        }
    }

    private fun Expression.isNegative(): Boolean {
        return when {
            isUnaryExpr -> asUnaryExpr().operator == UnaryExpr.Operator.MINUS
            isIntegerLiteralExpr -> when (val n = asIntegerLiteralExpr().asNumber()) {
                IntegerLiteralExpr.MAX_31_BIT_UNSIGNED_VALUE_AS_LONG -> false
                is Int -> n < 0
                else -> error("Invalid IntegerLiteralExpr type")
            }
            isLongLiteralExpr -> when (val n = asLongLiteralExpr().asNumber()) {
                LongLiteralExpr.MAX_63_BIT_UNSIGNED_VALUE_AS_BIG_INTEGER -> false
                is Long -> n < 0
                else -> error("Invalid LongLiteralExpr type")
            }
            else -> false
        }
    }
}
