package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit

abstract class Transformer {
    fun transform(units: Map<String, CompilationUnit>) {
        preTransform(units)

        for (unit in units.values) {
            transformUnit(units, unit)
        }

        postTransform(units)
    }

    protected open fun preTransform(units: Map<String, CompilationUnit>) {
        // empty
    }

    protected open fun transformUnit(units: Map<String, CompilationUnit>, unit: CompilationUnit) {
        // empty
    }

    protected open fun postTransform(units: Map<String, CompilationUnit>) {
        // empty
    }
}
