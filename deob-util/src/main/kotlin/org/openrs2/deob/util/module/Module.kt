package org.openrs2.deob.util.module

import java.nio.file.Path

public class Module(
    public val type: ModuleType,
    public val source: String,
    public val format: ModuleFormat,
    directory: Path,
    public val dependencies: Set<Module> = emptySet(),
) {
    public val name: String = type.library
    public val jar: Path = directory.resolve("var/cache/deob").resolve("$name.jar")
    public val sources: Path = directory.resolve(name).resolve("src/main/java")
    public val transitiveDependencies: Set<Module> = dependencies + dependencies.flatMap(Module::transitiveDependencies)
}
