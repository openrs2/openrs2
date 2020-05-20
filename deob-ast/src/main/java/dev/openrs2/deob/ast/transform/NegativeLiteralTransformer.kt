package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.util.isIntegerOrLongLiteral
import dev.openrs2.deob.ast.util.negate
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class NegativeLiteralTransformer : Transformer() {
    override fun transformUnit(
        units: Map<String, CompilationUnit>,
        unit: CompilationUnit
    ) {
        unit.walk { expr: UnaryExpr ->
            val operand = expr.expression
            if (!operand.isIntegerOrLongLiteral()) {
                return@walk
            }

            @Suppress("NON_EXHAUSTIVE_WHEN")
            when (expr.operator) {
                UnaryExpr.Operator.PLUS -> expr.replace(operand)
                UnaryExpr.Operator.MINUS -> expr.replace(operand.negate())
            }
        }
    }
}
