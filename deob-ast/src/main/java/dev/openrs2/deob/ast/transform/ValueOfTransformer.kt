package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.TypeExpr
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class ValueOfTransformer : Transformer() {
    override fun transformUnit(
        units: Map<String, CompilationUnit>,
        unit: CompilationUnit
    ) {
        unit.walk { expr: ObjectCreationExpr ->
            if (expr.type.isBoxedType) {
                expr.replace(MethodCallExpr(TypeExpr(expr.type), "valueOf", expr.arguments))
            }
        }
    }
}
