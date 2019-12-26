package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.Node
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.TypeExpr
import dev.openrs2.deob.ast.util.walk

class ValueOfTransformer : Transformer() {
    override fun transform(unit: CompilationUnit) {
        unit.walk(Node.TreeTraversal.POSTORDER) { expr: ObjectCreationExpr ->
            if (expr.type.isBoxedType) {
                expr.replace(MethodCallExpr(TypeExpr(expr.type), "valueOf", expr.arguments))
            }
        }
    }
}
