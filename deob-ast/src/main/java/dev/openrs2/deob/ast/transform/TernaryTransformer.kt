package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.ConditionalExpr
import dev.openrs2.deob.ast.util.ExprUtils
import dev.openrs2.deob.ast.util.NodeUtils

class TernaryTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, ConditionalExpr::class.java) { expr ->
            val condition = expr.condition
            val notCondition = ExprUtils.not(condition)
            if (ExprUtils.countNots(notCondition) >= ExprUtils.countNots(condition)) {
                return@walk
            }

            val thenExpr = expr.thenExpr
            val elseExpr = expr.elseExpr

            expr.condition = notCondition

            expr.thenExpr = elseExpr.clone()
            expr.elseExpr = thenExpr.clone()
        }
    }
}
