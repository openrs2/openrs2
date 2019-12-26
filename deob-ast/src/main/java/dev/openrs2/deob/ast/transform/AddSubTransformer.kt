package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.hasSideEffects
import dev.openrs2.deob.ast.util.isString
import dev.openrs2.deob.ast.util.negate
import dev.openrs2.deob.ast.util.walk

class AddSubTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: BinaryExpr ->
            val op = expr.operator
            val left = expr.left
            val right = expr.right
            val type = expr.calculateResolvedType()

            if (op == BinaryExpr.Operator.PLUS && type.isString()) {
                return@walk
            }

            if (op == BinaryExpr.Operator.PLUS && isNegative(right)) {
                // x + -y => x - y
                expr.operator = BinaryExpr.Operator.MINUS
                expr.right = right.negate()
            } else if (op == BinaryExpr.Operator.PLUS && isNegative(left)) {
                if (left.hasSideEffects() || right.hasSideEffects()) {
                    return@walk
                }

                // -x + y => y - x
                expr.operator = BinaryExpr.Operator.MINUS
                expr.left = right.clone()
                expr.right = left.negate()
            } else if (op == BinaryExpr.Operator.MINUS && isNegative(right)) {
                // x - -y => x + y
                expr.operator = BinaryExpr.Operator.PLUS
                expr.right = right.negate()
            }
        }
    }

    companion object {
        private fun isNegative(expr: Expression): Boolean {
            return when {
                expr.isUnaryExpr -> expr.asUnaryExpr().operator == UnaryExpr.Operator.MINUS
                expr.isIntegerLiteralExpr -> expr.asIntegerLiteralExpr().asInt() < 0
                expr.isLongLiteralExpr -> expr.asLongLiteralExpr().asLong() < 0
                else -> false
            }
        }
    }
}
