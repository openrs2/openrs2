package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ForStmt
import dev.openrs2.deob.ast.util.walk

class IncrementTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk { stmt: ExpressionStmt ->
            if (!stmt.expression.isUnaryExpr) {
                return@walk
            }

            val unaryExpr = stmt.expression.asUnaryExpr()
            unaryExpr.operator = unaryExpr.operator.toPostfix()
        }

        unit.walk { stmt: ForStmt ->
            for (expr in stmt.update) {
                if (!expr.isUnaryExpr) {
                    continue
                }

                val unaryExpr = expr.asUnaryExpr()
                unaryExpr.operator = unaryExpr.operator.toPostfix()
            }
        }
    }

    private fun UnaryExpr.Operator.toPostfix() = when (this) {
        UnaryExpr.Operator.PREFIX_INCREMENT -> UnaryExpr.Operator.POSTFIX_INCREMENT
        UnaryExpr.Operator.PREFIX_DECREMENT -> UnaryExpr.Operator.POSTFIX_DECREMENT
        else -> this
    }
}
