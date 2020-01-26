package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.stmt.ForStmt
import dev.openrs2.deob.ast.util.walk

class ForLoopConditionTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { stmt: ForStmt ->
            stmt.compare.ifPresent { compare ->
                if (!compare.isBinaryExpr) {
                    return@ifPresent
                }

                val expr = compare.asBinaryExpr()
                val flipped = when (expr.operator) {
                    BinaryExpr.Operator.GREATER -> BinaryExpr.Operator.LESS
                    BinaryExpr.Operator.GREATER_EQUALS -> BinaryExpr.Operator.LESS_EQUALS
                    else -> return@ifPresent
                }

                stmt.setCompare(BinaryExpr(expr.right, expr.left, flipped))
            }
        }
    }
}
