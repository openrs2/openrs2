package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.stmt.ExpressionStmt
import com.github.javaparser.ast.stmt.ForStmt
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class IncrementTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { stmt: ExpressionStmt ->
            val expr = stmt.expression
            if (expr !is UnaryExpr) {
                return@walk
            }

            expr.operator = expr.operator.toPostfix()
        }

        unit.walk { stmt: ForStmt ->
            for (expr in stmt.update) {
                if (expr !is UnaryExpr) {
                    continue
                }

                expr.operator = expr.operator.toPostfix()
            }
        }
    }

    private fun UnaryExpr.Operator.toPostfix() = when (this) {
        UnaryExpr.Operator.PREFIX_INCREMENT -> UnaryExpr.Operator.POSTFIX_INCREMENT
        UnaryExpr.Operator.PREFIX_DECREMENT -> UnaryExpr.Operator.POSTFIX_DECREMENT
        else -> this
    }
}
