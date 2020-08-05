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
}
