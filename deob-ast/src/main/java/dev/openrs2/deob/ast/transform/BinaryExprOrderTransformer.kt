package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.BinaryExpr
import com.github.javaparser.ast.expr.Expression
import com.github.javaparser.ast.expr.LiteralExpr
import com.github.javaparser.ast.expr.ThisExpr
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup
import dev.openrs2.deob.ast.util.flip
import dev.openrs2.deob.ast.util.isString
import dev.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class BinaryExprOrderTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: BinaryExpr ->
            val op = expr.operator.flip() ?: return@walk

            val type = expr.calculateResolvedType()
            if (op == BinaryExpr.Operator.PLUS && type.isString()) {
                return@walk
            }

            val left = expr.left
            val right = expr.right
            if (left.isLiteralOrThisExpr && !right.isLiteralOrThisExpr) {
                expr.operator = op
                expr.left = right.clone()
                expr.right = left.clone()
            }
        }
    }

    private val Expression.isLiteralOrThisExpr: Boolean
        get() = this is LiteralExpr || this is ThisExpr
}
