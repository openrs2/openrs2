package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import dev.openrs2.deob.ast.util.isClass
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class NewInstanceTransformer : Transformer() {
    override fun transformUnit(
        units: Map<String, CompilationUnit>,
        unit: CompilationUnit
    ) {
        unit.walk { expr: MethodCallExpr ->
            if (expr.nameAsString != "newInstance") {
                return@walk
            }

            expr.scope.ifPresent { scope ->
                if (scope.calculateResolvedType().isClass()) {
                    expr.setScope(MethodCallExpr(scope.clone(), "getDeclaredConstructor"))
                }
            }
        }
    }
}
