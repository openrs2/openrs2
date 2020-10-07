package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.UnaryExpr
import com.github.javaparser.ast.expr.UnaryExpr.Operator.POSTFIX_DECREMENT
import com.github.javaparser.ast.expr.UnaryExpr.Operator.POSTFIX_INCREMENT
import com.github.javaparser.ast.expr.UnaryExpr.Operator.PREFIX_DECREMENT
import com.github.javaparser.ast.expr.UnaryExpr.Operator.PREFIX_INCREMENT
import com.github.javaparser.ast.stmt.ForStmt
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.flip
import org.openrs2.deob.ast.util.hasSideEffects
import org.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class ForLoopConditionTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { stmt: ForStmt ->
            val updatedExprs = stmt.update.mapNotNull { it.getUpdatedExpr() }

            stmt.compare.ifPresent { expr ->
                if (expr !is BinaryExpr) {
                    return@ifPresent
                } else if (expr.hasSideEffects()) {
                    return@ifPresent
                }

                val flipped = expr.operator.flip() ?: return@ifPresent

                if (expr.left !in updatedExprs && expr.right in updatedExprs) {
                    stmt.setCompare(BinaryExpr(expr.right, expr.left, flipped))
                }
            }
        }
    }

    private fun Expression.getUpdatedExpr(): Expression? {
        return when (this) {
            is UnaryExpr -> when (operator) {
                PREFIX_INCREMENT, PREFIX_DECREMENT, POSTFIX_INCREMENT, POSTFIX_DECREMENT -> expression
                else -> null
            }
            is AssignExpr -> target
            else -> null
        }
    }
}
