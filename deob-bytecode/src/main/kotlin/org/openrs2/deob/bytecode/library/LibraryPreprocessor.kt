package org.openrs2.deob.bytecode.library

import org.openrs2.asm.classpath.Library
import org.openrs2.deob.util.module.Module
import org.openrs2.deob.util.module.ModuleType

public abstract class LibraryPreprocessor(public val requiredModules: Set<ModuleType>) {
    public val name: String = javaClass.simpleName.removeSuffix("Preprocessor")

    public constructor(vararg modules: ModuleType) : this(modules.toSet())
    public abstract fun preprocess(modules: Map<ModuleType, Module>, libraries: Map<ModuleType, Library>)
}
