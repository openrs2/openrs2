package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.ExprUtils
import dev.openrs2.deob.ast.util.NodeUtils

class NegativeLiteralTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, UnaryExpr::class.java) { expr ->
            val operand = expr.expression
            if (!ExprUtils.isIntegerOrLongLiteral(operand)) {
                return@walk
            }

            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (expr.operator) {
                UnaryExpr.Operator.PLUS -> {
                    expr.replace(operand)
                }
                UnaryExpr.Operator.MINUS -> {
                    expr.replace(ExprUtils.negate(operand))
                }
            }
        }
    }
}
