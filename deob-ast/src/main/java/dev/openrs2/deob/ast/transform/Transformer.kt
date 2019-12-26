package dev.openrs2.deob.ast.transform

import com.github.javaparser.ast.CompilationUnit

abstract class Transformer {
    abstract fun transform(unit: CompilationUnit)
}
