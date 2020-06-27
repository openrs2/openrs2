package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class IdentityTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            @Suppress("NON_EXHAUSTIVE_WHEN")
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
            }
        }
    }

    private fun Expression.isZero(): Boolean {
        return when {
            isIntegerLiteralExpr -> asIntegerLiteralExpr().asNumber() == 0
            isLongLiteralExpr -> asLongLiteralExpr().asNumber() == 0L
            else -> false
        }
    }

    private fun Expression.isOne(): Boolean {
        return when {
            isIntegerLiteralExpr -> asIntegerLiteralExpr().asNumber() == 1
            isLongLiteralExpr -> asLongLiteralExpr().asNumber() == 1L
            else -> false
        }
    }
}
