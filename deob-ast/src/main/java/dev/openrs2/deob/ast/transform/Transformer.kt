package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit
import dev.openrs2.deob.ast.Library
import dev.openrs2.deob.ast.LibraryGroup

public abstract class Transformer {
    public fun transform(group: LibraryGroup) {
        preTransform(group)

        for (library in group) {
            for (unit in library) {
                transformUnit(group, library, unit)
            }
        }

        postTransform(group)
    }

    protected open fun preTransform(group: LibraryGroup) {
        // empty
    }

    protected open fun transformUnit(group: LibraryGroup, library: Library, unit: CompilationUnit) {
        // empty
    }

    protected open fun postTransform(group: LibraryGroup) {
        // empty
    }
}
