package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.EnclosedExpr
import dev.openrs2.deob.ast.util.NodeUtils

class UnencloseTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        NodeUtils.walk(unit, Node.TreeTraversal.POSTORDER, EnclosedExpr::class.java) { expr ->
            expr.replace(expr.inner.clone())
        }
    }
}
