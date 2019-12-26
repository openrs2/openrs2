package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import dev.openrs2.deob.ast.util.NodeUtils
import dev.openrs2.deob.ast.util.TypeUtils

class BinaryExprOrderTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, BinaryExpr::class.java) { expr ->
            val op = flip(expr.operator) ?: return@walk

            val type = expr.calculateResolvedType()
            if (op == BinaryExpr.Operator.PLUS && TypeUtils.isString(type)) {
                return@walk
            }

            val left = expr.left
            val right = expr.right
            if (left.isLiteralExpr && !right.isLiteralExpr) {
                expr.operator = op
                expr.left = right.clone()
                expr.right = left.clone()
            }
        }
    }

    companion object {
        private fun flip(op: BinaryExpr.Operator): BinaryExpr.Operator? {
            return when (op) {
                BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MULTIPLY -> op
                BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> op
                BinaryExpr.Operator.BINARY_AND, BinaryExpr.Operator.BINARY_OR -> op
                BinaryExpr.Operator.XOR, BinaryExpr.Operator.OR, BinaryExpr.Operator.AND -> op
                BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS
                BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS_EQUALS
                BinaryExpr.Operator.LESS -> BinaryExpr.Operator.GREATER
                BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr.Operator.GREATER_EQUALS
                else -> null
            }
        }
    }
}
