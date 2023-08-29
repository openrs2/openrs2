package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.ConditionalExpr
import jakarta.inject.Singleton
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.countNots
import org.openrs2.deob.ast.util.not
import org.openrs2.deob.ast.util.walk

@Singleton
public class TernaryTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: ConditionalExpr ->
            val condition = expr.condition
            val notCondition = condition.not()
            if (notCondition.countNots() >= condition.countNots()) {
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
