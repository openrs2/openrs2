package org.openrs2.deob.util

import java.nio.file.Path

public class Module(public val name: String, public val dependencies: Set<Module> = emptySet()) {
    public val jar: Path = Path.of("nonfree/var/cache/deob").resolve("$name.jar")
    public val sources: Path = Path.of("nonfree").resolve(name).resolve("src/main/java")
    public val transitiveDependencies: Set<Module> =
        dependencies.plus(dependencies.flatMap { it.transitiveDependencies })
}
