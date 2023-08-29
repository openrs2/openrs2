package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.BinaryExpr.Operator.EQUALS
import com.github.javaparser.ast.expr.BinaryExpr.Operator.NOT_EQUALS
import com.github.javaparser.ast.expr.BooleanLiteralExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.UnaryExpr.Operator.LOGICAL_COMPLEMENT
import jakarta.inject.Singleton
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.not
import org.openrs2.deob.ast.util.walk

@Singleton
public class NotTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val op = expr.operator.flip() ?: return@walk
            val left = expr.left
            val right = expr.right

            val bothLiteral = left is BooleanLiteralExpr && right is BooleanLiteralExpr
            if (bothLiteral) {
                return@walk
            }

            val leftNotOrLiteral = left.isNotOrLiteral()
            val rightNotOrLiteral = right.isNotOrLiteral()

            if (leftNotOrLiteral && rightNotOrLiteral) {
                expr.left = left.not()
                expr.right = right.not()
                return@walk
            } else if (leftNotOrLiteral) {
                expr.operator = op
                expr.left = left.not()
            } else if (rightNotOrLiteral) {
                expr.operator = op
                expr.right = right.not()
            }
        }
    }

    private fun Expression.isNot(): Boolean {
        return this is UnaryExpr && operator == LOGICAL_COMPLEMENT
    }

    private fun Expression.isNotOrLiteral(): Boolean {
        return isNot() || this is BooleanLiteralExpr
    }

    private fun BinaryExpr.Operator.flip(): BinaryExpr.Operator? {
        return when (this) {
            EQUALS -> NOT_EQUALS
            NOT_EQUALS -> EQUALS
            else -> null
        }
    }
}
