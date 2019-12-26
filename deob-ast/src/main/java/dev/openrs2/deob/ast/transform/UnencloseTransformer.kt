package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.EnclosedExpr
import dev.openrs2.deob.ast.util.walk

class UnencloseTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: EnclosedExpr ->
            expr.replace(expr.inner.clone())
        }
    }
}
