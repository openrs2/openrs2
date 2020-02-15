package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.checkedAsInt
import dev.openrs2.deob.ast.util.checkedAsLong
import dev.openrs2.deob.ast.util.createLong
import dev.openrs2.deob.ast.util.isIntegerOrLongLiteral
import dev.openrs2.deob.ast.util.walk

class ComplementTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val op = complement(expr.operator) ?: return@walk

            val left = expr.left
            val right = expr.right
            val bothLiteral = left.isIntegerOrLongLiteral() && right.isIntegerOrLongLiteral()

            if (left.isComplementOrLiteral() && right.isComplementOrLiteral() && !bothLiteral) {
                expr.operator = op
                expr.left = left.complement()
                expr.right = right.complement()
            }
        }
    }

    companion object {
        private fun Expression.isComplement(): Boolean {
            return isUnaryExpr && asUnaryExpr().operator == UnaryExpr.Operator.BITWISE_COMPLEMENT
        }

        private fun Expression.isComplementOrLiteral(): Boolean {
            return isComplement() || isIntegerOrLongLiteral()
        }

        private fun complement(op: BinaryExpr.Operator): BinaryExpr.Operator? {
            return when (op) {
                BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> op
                BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS
                BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS_EQUALS
                BinaryExpr.Operator.LESS -> BinaryExpr.Operator.GREATER
                BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr.Operator.GREATER_EQUALS
                else -> null
            }
        }

        private fun Expression.complement(): Expression {
            return when {
                isUnaryExpr -> asUnaryExpr().expression
                isIntegerLiteralExpr -> IntegerLiteralExpr(asIntegerLiteralExpr().checkedAsInt().inv().toString())
                isLongLiteralExpr -> createLong(asLongLiteralExpr().checkedAsLong().inv())
                else -> throw IllegalArgumentException()
            }
        }
    }
}
