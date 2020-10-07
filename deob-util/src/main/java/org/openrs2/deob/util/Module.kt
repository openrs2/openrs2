package org.openrs2.deob.util

import java.nio.file.Path
import java.nio.file.Paths

public class Module(public val name: String, public val dependencies: Set<Module> = emptySet()) {
    public val jar: Path = Paths.get("nonfree/var/cache/deob").resolve("$name.jar")
    public val sources: Path = Paths.get("nonfree").resolve(name).resolve("src/main/java")
    public val transitiveDependencies: Set<Module> =
        dependencies.plus(dependencies.flatMap { it.transitiveDependencies })

    public companion object {
        private val GL = Module("gl")
        private val SIGNLINK = Module("signlink")
        private val UNPACK = Module("unpack")
        private val CLIENT = Module("client", setOf(GL, SIGNLINK))
        private val LOADER = Module("loader", setOf(SIGNLINK, UNPACK))
        private val UNPACKCLASS = Module("unpackclass", setOf(UNPACK))

        public val ALL: Set<Module> = setOf(CLIENT, GL, LOADER, SIGNLINK, UNPACK, UNPACKCLASS)
    }
}
