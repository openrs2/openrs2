package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.EnclosedExpr
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
class UnencloseTransformer : Transformer() {
    override fun transformUnit(
        units: Map<String, CompilationUnit>,
        unit: CompilationUnit
    ) {
        unit.walk { expr: EnclosedExpr ->
            expr.replace(expr.inner.clone())
        }
    }
}
