package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.walk

class IdentityTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: BinaryExpr ->
            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (expr.operator) {
                BinaryExpr.Operator.PLUS -> {
                    if (isZero(expr.left)) {
                        // 0 + x => x
                        expr.replace(expr.right)
                    } else if (isZero(expr.right)) {
                        // x + 0 => x
                        expr.replace(expr.left)
                    }
                }
                BinaryExpr.Operator.MINUS -> {
                    if (isZero(expr.left)) {
                        // 0 - x => -x
                        expr.replace(UnaryExpr(expr.right, UnaryExpr.Operator.MINUS))
                    } else if (isZero(expr.right)) {
                        // x - 0 => x
                        expr.replace(expr.left)
                    }
                }
                BinaryExpr.Operator.MULTIPLY -> {
                    if (isOne(expr.left)) {
                        // 1 * x => x
                        expr.replace(expr.right)
                    } else if (isOne(expr.right)) {
                        // x * 1 => x
                        expr.replace(expr.left)
                    }
                }
            }
        }
    }

    companion object {
        private fun isZero(expr: Expression): Boolean {
            return when {
                expr.isIntegerLiteralExpr -> expr.asIntegerLiteralExpr().asNumber() == 0
                expr.isLongLiteralExpr -> expr.asLongLiteralExpr().asNumber() == 0L
                else -> false
            }
        }

        private fun isOne(expr: Expression): Boolean {
            return when {
                expr.isIntegerLiteralExpr -> expr.asIntegerLiteralExpr().asNumber() == 1
                expr.isLongLiteralExpr -> expr.asLongLiteralExpr().asNumber() == 1L
                else -> false
            }
        }
    }
}
