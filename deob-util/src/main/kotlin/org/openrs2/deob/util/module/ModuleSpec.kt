package org.openrs2.deob.util.module

public data class ModuleSpec(
    public val type: ModuleType,
    public val source: String = type.library,
    public val format: ModuleFormat = ModuleFormat.fromPath(source),
    public val dependencies: Set<ModuleType> = emptySet()
)
