package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import dev.openrs2.deob.ast.util.isString
import dev.openrs2.deob.ast.util.walk

class BinaryExprOrderTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val op = expr.operator.flip() ?: return@walk

            val type = expr.calculateResolvedType()
            if (op == BinaryExpr.Operator.PLUS && type.isString()) {
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

    private fun BinaryExpr.Operator.flip(): BinaryExpr.Operator? {
        return when (this) {
            BinaryExpr.Operator.PLUS, BinaryExpr.Operator.MULTIPLY -> this
            BinaryExpr.Operator.EQUALS, BinaryExpr.Operator.NOT_EQUALS -> this
            BinaryExpr.Operator.BINARY_AND, BinaryExpr.Operator.BINARY_OR -> this
            BinaryExpr.Operator.XOR, BinaryExpr.Operator.OR, BinaryExpr.Operator.AND -> this
            BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS
            BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS_EQUALS
            BinaryExpr.Operator.LESS -> BinaryExpr.Operator.GREATER
            BinaryExpr.Operator.LESS_EQUALS -> BinaryExpr.Operator.GREATER_EQUALS
            else -> null
        }
    }
}
