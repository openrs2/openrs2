package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.ExprUtils
import dev.openrs2.deob.ast.util.TypeUtils
import dev.openrs2.deob.ast.util.walk

class AddSubTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: BinaryExpr ->
            val op = expr.operator
            val left = expr.left
            val right = expr.right
            val type = expr.calculateResolvedType()

            if (op == BinaryExpr.Operator.PLUS && TypeUtils.isString(type)) {
                return@walk
            }

            if (op == BinaryExpr.Operator.PLUS && isNegative(right)) {
                // x + -y => x - y
                expr.operator = BinaryExpr.Operator.MINUS
                expr.right = ExprUtils.negate(right)
            } else if (op == BinaryExpr.Operator.PLUS && isNegative(left)) {
                if (ExprUtils.hasSideEffects(left) || ExprUtils.hasSideEffects(right)) {
                    return@walk
                }

                // -x + y => y - x
                expr.operator = BinaryExpr.Operator.MINUS
                expr.left = right.clone()
                expr.right = ExprUtils.negate(left)
            } else if (op == BinaryExpr.Operator.MINUS && isNegative(right)) {
                // x - -y => x + y
                expr.operator = BinaryExpr.Operator.PLUS
                expr.right = ExprUtils.negate(right)
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
