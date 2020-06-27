package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.stmt.ForStmt
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.hasSideEffects
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class ForLoopConditionTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { stmt: ForStmt ->
            stmt.compare.ifPresent { compare ->
                if (!compare.isBinaryExpr) {
                    return@ifPresent
                }

                val expr = compare.asBinaryExpr()
                if (expr.hasSideEffects()) {
                    return@ifPresent
                }

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
