package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.ExprUtils
import dev.openrs2.deob.ast.util.NodeUtils

class ComplementTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, BinaryExpr::class.java) { expr ->
            val op = complement(expr.operator) ?: return@walk

            val left = expr.left
            val right = expr.right
            val bothLiteral = ExprUtils.isIntegerOrLongLiteral(left) && ExprUtils.isIntegerOrLongLiteral(right)

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
            return isComplement(expr) || ExprUtils.isIntegerOrLongLiteral(expr)
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
                expr.isLongLiteralExpr -> ExprUtils.createLong(expr.asLongLiteralExpr().asLong().inv())
                else -> throw IllegalArgumentException()
            }
        }
    }
}
