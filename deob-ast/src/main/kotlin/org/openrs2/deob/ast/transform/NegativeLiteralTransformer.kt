package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.UnaryExpr
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.isIntegerOrLongLiteral
import org.openrs2.deob.ast.util.negate
import org.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class NegativeLiteralTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: UnaryExpr ->
            val operand = expr.expression
            if (!operand.isIntegerOrLongLiteral()) {
                return@walk
            }

            when (expr.operator) {
                UnaryExpr.Operator.PLUS -> expr.replace(operand)
                UnaryExpr.Operator.MINUS -> expr.replace(operand.negate())
                else -> Unit
            }
        }
    }
}
