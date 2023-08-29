package org.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import com.github.javaparser.ast.expr.MethodCallExpr
import jakarta.inject.Singleton
import org.openrs2.deob.ast.Library
import org.openrs2.deob.ast.LibraryGroup
import org.openrs2.deob.ast.util.isClass
import org.openrs2.deob.ast.util.walk

@Singleton
public class NewInstanceTransformer : Transformer() {
    override fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
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
