package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.AssignExpr
import com.github.javaparser.ast.expr.CastExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.NullLiteralExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.VariableDeclarationExpr
import com.github.javaparser.resolution.MethodAmbiguityException
import com.github.javaparser.resolution.declarations.ResolvedMethodLikeDeclaration
import com.github.javaparser.resolution.types.ResolvedPrimitiveType
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.BYTE
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.CHAR
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.DOUBLE
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.FLOAT
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.INT
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.LONG
import com.github.javaparser.resolution.types.ResolvedPrimitiveType.SHORT
import com.google.common.collect.ImmutableSetMultimap
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class RedundantCastTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        // remove double casts
        unit.walk { expr: CastExpr ->
            val innerExpr = expr.expression
            if (innerExpr is CastExpr && expr.type == innerExpr.type) {
                expr.expression = innerExpr.expression.clone()
            }
        }

        // remove null argument casts if the call remains unambiguous
        unit.walk { expr: MethodCallExpr ->
            for (i in expr.arguments.indices) {
                val arg = expr.arguments[i]
                if (!isCastedNull(arg)) {
                    continue
                } else if (resolvesVariadicAmbiguity(i, expr.resolve(), arg as CastExpr)) {
                    continue
                }

                expr.arguments[i] = NullLiteralExpr()
                try {
                    expr.resolve()
                } catch (ex: MethodAmbiguityException) {
                    expr.arguments[i] = arg
                }
            }
        }

        unit.walk { expr: ObjectCreationExpr ->
            for (i in expr.arguments.indices) {
                val arg = expr.arguments[i]
                if (!isCastedNull(arg)) {
                    continue
                } else if (resolvesVariadicAmbiguity(i, expr.resolve(), arg as CastExpr)) {
                    continue
                }

                expr.arguments[i] = NullLiteralExpr()
                try {
                    expr.resolve()
                } catch (ex: MethodAmbiguityException) {
                    expr.arguments[i] = arg
                }
            }
        }

        // remove null assignment casts
        unit.walk { expr: VariableDeclarationExpr ->
            for (variable in expr.variables) {
                variable.initializer.ifPresent { initializer ->
                    if (isCastedNull(initializer)) {
                        initializer.replace(NullLiteralExpr())
                    }
                }
            }
        }

        unit.walk { expr: AssignExpr ->
            if (isCastedNull(expr.value)) {
                expr.value = NullLiteralExpr()
            }
        }

        // replace casts with widening/narrowing conversions
        // see https://docs.oracle.com/javase/specs/jls/se11/html/jls-5.html
        unit.walk { expr: CastExpr ->
            expr.parentNode.ifPresent { parent ->
                if (parent !is AssignExpr && parent !is CastExpr) {
                    return@ifPresent
                }

                val outerType = expr.type
                if (!outerType.isPrimitiveType) {
                    return@ifPresent
                }

                val innerType = expr.expression.calculateResolvedType()
                if (!innerType.isPrimitive) {
                    return@ifPresent
                }

                val resolvedOuterType = outerType.resolve()

                if (WIDENING_CONVERSIONS.containsEntry(innerType, resolvedOuterType)) {
                    expr.replace(expr.expression.clone())
                } else if (NARROWING_CONVERSIONS.containsEntry(innerType, resolvedOuterType) && parent is CastExpr) {
                    expr.replace(expr.expression.clone())
                } else if (innerType == BYTE && resolvedOuterType == CHAR && parent is CastExpr) {
                    expr.replace(expr.expression.clone())
                }
            }
        }
    }

    private fun isCastedNull(expr: Expression): Boolean {
        if (expr !is CastExpr) {
            return false
        }
        return expr.expression is NullLiteralExpr
    }

    private fun resolvesVariadicAmbiguity(index: Int, method: ResolvedMethodLikeDeclaration, cast: CastExpr): Boolean {
        if (index < method.numberOfParams) {
            val param = method.getParam(index)
            if (param.isVariadic && param.type == cast.type.resolve()) {
                return true
            }
        }

        return false
    }

    private companion object {
        private val WIDENING_CONVERSIONS = ImmutableSetMultimap.builder<ResolvedPrimitiveType, ResolvedPrimitiveType>()
            .putAll(BYTE, SHORT, INT, LONG, FLOAT, DOUBLE)
            .putAll(SHORT, INT, LONG, FLOAT, DOUBLE)
            .putAll(CHAR, INT, LONG, FLOAT, DOUBLE)
            .putAll(INT, LONG, FLOAT, DOUBLE)
            .putAll(LONG, FLOAT, DOUBLE)
            .putAll(FLOAT, DOUBLE)
            .build()

        private val NARROWING_CONVERSIONS = ImmutableSetMultimap.builder<ResolvedPrimitiveType, ResolvedPrimitiveType>()
            .putAll(SHORT, BYTE, CHAR)
            .putAll(CHAR, BYTE, SHORT)
            .putAll(INT, BYTE, SHORT, CHAR)
            .putAll(LONG, BYTE, SHORT, CHAR, INT)
            .putAll(FLOAT, BYTE, SHORT, CHAR, INT, LONG)
            .putAll(DOUBLE, BYTE, SHORT, CHAR, INT, LONG, FLOAT)
            .build()
    }
}
