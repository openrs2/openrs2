package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class IdentityTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            when (expr.operator) {
                BinaryExpr.Operator.PLUS -> {
                    if (expr.left.isZero()) {
                        // 0 + x => x
                        expr.replace(expr.right)
                    } else if (expr.right.isZero()) {
                        // x + 0 => x
                        expr.replace(expr.left)
                    }
                }

                BinaryExpr.Operator.MINUS -> {
                    if (expr.left.isZero()) {
                        // 0 - x => -x
                        expr.replace(UnaryExpr(expr.right, UnaryExpr.Operator.MINUS))
                    } else if (expr.right.isZero()) {
                        // x - 0 => x
                        expr.replace(expr.left)
                    }
                }

                BinaryExpr.Operator.MULTIPLY -> {
                    if (expr.left.isOne()) {
                        // 1 * x => x
                        expr.replace(expr.right)
                    } else if (expr.right.isOne()) {
                        // x * 1 => x
                        expr.replace(expr.left)
                    }
                }

                BinaryExpr.Operator.DIVIDE -> {
                    if (expr.right.isOne()) {
                        // x / 1 => x
                        expr.replace(expr.left)
                    }
                }

                else -> Unit
            }
        }

        unit.walk { expr: AssignExpr ->
            val identity = when (expr.operator) {
                // x += 0, x -= 0
                AssignExpr.Operator.PLUS, AssignExpr.Operator.MINUS -> expr.value.isZero()
                // x *= 1, x /= 1
                AssignExpr.Operator.MULTIPLY, AssignExpr.Operator.DIVIDE -> expr.value.isOne()
                else -> false
            }

            if (!identity) {
                return@walk
            }

            expr.parentNode.ifPresent { parent ->
                if (parent is ExpressionStmt) {
                    parent.remove()
                } else {
                    expr.replace(expr.target.clone())
                }
            }
        }
    }

    private fun Expression.isZero(): Boolean {
        return when (this) {
            is IntegerLiteralExpr -> asNumber() == 0
            is LongLiteralExpr -> asNumber() == 0L
            else -> false
        }
    }

    private fun Expression.isOne(): Boolean {
        return when (this) {
            is IntegerLiteralExpr -> asNumber() == 1
            is LongLiteralExpr -> asNumber() == 1L
            else -> false
        }
    }
}
