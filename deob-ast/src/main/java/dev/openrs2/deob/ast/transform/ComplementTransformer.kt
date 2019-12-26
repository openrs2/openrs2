package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.createLong
import dev.openrs2.deob.ast.util.isIntegerOrLongLiteral
import dev.openrs2.deob.ast.util.walk

class ComplementTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: BinaryExpr ->
            val op = complement(expr.operator) ?: return@walk

            val left = expr.left
            val right = expr.right
            val bothLiteral = left.isIntegerOrLongLiteral() && right.isIntegerOrLongLiteral()

            if (isComplementOrLiteral(left) && isComplementOrLiteral(right) && !bothLiteral) {
                expr.operator = op
                expr.left = complement(left)
                expr.right = complement(right)
            }
        }
    }

    companion object {
        private fun isComplement(expr: Expression): Boolean {
            return expr.isUnaryExpr && expr.asUnaryExpr().operator == UnaryExpr.Operator.BITWISE_COMPLEMENT
        }

        private fun isComplementOrLiteral(expr: Expression): Boolean {
            return isComplement(expr) || expr.isIntegerOrLongLiteral()
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

        private fun complement(expr: Expression): Expression {
            return when {
                expr.isUnaryExpr -> expr.asUnaryExpr().expression
                expr.isIntegerLiteralExpr -> IntegerLiteralExpr(expr.asIntegerLiteralExpr().asInt().inv())
                expr.isLongLiteralExpr -> createLong(expr.asLongLiteralExpr().asLong().inv())
                else -> throw IllegalArgumentException()
            }
        }
    }
}
