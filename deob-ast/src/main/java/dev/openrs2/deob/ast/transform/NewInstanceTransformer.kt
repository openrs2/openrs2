package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.MethodCallExpr
import dev.openrs2.deob.ast.util.NodeUtils

class NewInstanceTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, MethodCallExpr::class.java) { expr ->
            if (expr.nameAsString != "newInstance") {
                return@walk
            }

            expr.scope.ifPresent { scope ->
                if (!scope.isMethodCallExpr || scope.asMethodCallExpr().nameAsString !in CONSTRUCTOR_METHODS) {
                    expr.setScope(MethodCallExpr(scope.clone(), "getDeclaredConstructor"))
                }
            }
        }
    }

    companion object {
        val CONSTRUCTOR_METHODS = setOf("getConstructor", "getDeclaredConstructor")
    }
}
