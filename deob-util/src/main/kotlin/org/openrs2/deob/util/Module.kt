package org.openrs2.deob.util

import java.nio.file.Path

public class Module(
    public val name: String,
    jars: Path,
    sources: Path,
    public val dependencies: Set<Module> = emptySet(),
) {
    public val jar: Path = jars.resolve("$name.jar")
    public val sources: Path = sources.resolve(name).resolve("src/main/java")
    public val transitiveDependencies: Set<Module> = dependencies + dependencies.flatMap(Module::transitiveDependencies)
}
