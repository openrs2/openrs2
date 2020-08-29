package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.IntegerLiteralExpr
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.LongLiteralExpr
import com.github.javaparser.ast.expr.UnaryExpr
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.hasSideEffects
import dev.openrs2.deob.ast.util.isString
import dev.openrs2.deob.ast.util.negate
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class AddSubTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val op = expr.operator
            if (op != BinaryExpr.Operator.PLUS && op != BinaryExpr.Operator.MINUS) {
                return@walk
            }

            val type = expr.calculateResolvedType()
            if (type.isString()) {
                return@walk
            }

            val terms = mutableListOf<Expression>()
            addTerms(terms, expr, negate = false)

            terms.sortWith(Comparator { a, b ->
                // preserve the order of adjacent expressions with side effects
                val aHasSideEffects = a.hasSideEffects()
                val bHasSideEffects = b.hasSideEffects()
                if (aHasSideEffects && bHasSideEffects) {
                    return@Comparator 0
                }

                // push negative expressions to the right so we can replace unary minus with binary minus
                val aNegative = a.isNegative()
                val bNegative = b.isNegative()
                if (aNegative && !bNegative) {
                    return@Comparator 1
                } else if (!aNegative && bNegative) {
                    return@Comparator -1
                }

                // push literals to the right
                val aLiteral = a is LiteralExpr
                val bLiteral = b is LiteralExpr
                if (aLiteral && !bLiteral) {
                    return@Comparator 1
                } else if (!aLiteral && bLiteral) {
                    return@Comparator -1
                }

                return@Comparator 0
            })

            val newExpr = terms.reduce { left, right ->
                if (right.isNegative()) {
                    BinaryExpr(left.clone(), right.negate(), BinaryExpr.Operator.MINUS)
                } else {
                    BinaryExpr(left.clone(), right.clone(), BinaryExpr.Operator.PLUS)
                }
            }

            expr.replace(newExpr)
        }
    }

    private fun addTerms(terms: MutableList<Expression>, expr: Expression, negate: Boolean) {
        when {
            expr is UnaryExpr -> when {
                expr.operator == UnaryExpr.Operator.MINUS -> addTerms(terms, expr.expression, !negate)
                negate -> terms += expr.negate()
                else -> terms += expr
            }
            expr is BinaryExpr -> when {
                expr.operator == BinaryExpr.Operator.PLUS -> {
                    addTerms(terms, expr.left, negate)
                    addTerms(terms, expr.right, negate)
                }
                expr.operator == BinaryExpr.Operator.MINUS -> {
                    addTerms(terms, expr.left, negate)
                    addTerms(terms, expr.right, !negate)
                }
                negate -> terms += expr.negate()
                else -> terms += expr
            }
            negate -> terms += expr.negate()
            else -> terms += expr
        }
    }

    private fun Expression.isNegative(): Boolean {
        return when (this) {
            is UnaryExpr -> operator == UnaryExpr.Operator.MINUS
            is IntegerLiteralExpr -> when (val n = asNumber()) {
                IntegerLiteralExpr.MAX_31_BIT_UNSIGNED_VALUE_AS_LONG -> false
                is Int -> n < 0
                else -> error("Invalid IntegerLiteralExpr type")
            }
            is LongLiteralExpr -> when (val n = asNumber()) {
                LongLiteralExpr.MAX_63_BIT_UNSIGNED_VALUE_AS_BIG_INTEGER -> false
                is Long -> n < 0
                else -> error("Invalid LongLiteralExpr type")
            }
            else -> false
        }
    }
}
