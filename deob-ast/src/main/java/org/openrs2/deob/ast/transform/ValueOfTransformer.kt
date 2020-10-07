package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import com.github.javaparser.ast.expr.ObjectCreationExpr
import com.github.javaparser.ast.expr.TypeExpr
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.walk
import javax.inject.Singleton

@Singleton
public class ValueOfTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        unit.walk { expr: ObjectCreationExpr ->
            if (expr.type.isBoxedType) {
                expr.replace(MethodCallExpr(TypeExpr(expr.type), "valueOf", expr.arguments))
            }
        }
    }
}
