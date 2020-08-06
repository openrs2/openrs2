package dev.openrs2.deob.util

import java.nio.file.Path
import java.nio.file.Paths

class Module(val name: String, val dependencies: Set<Module> = emptySet()) {
    val jar: Path = Paths.get("nonfree/var/cache/deob").resolve("$name.jar")
    val sources: Path = Paths.get("nonfree").resolve(name).resolve("src/main/java")
    val transitiveDependencies: Set<Module> = dependencies.plus(dependencies.flatMap { it.transitiveDependencies })

    companion object {
        private val GL = Module("gl")
        private val SIGNLINK = Module("signlink")
        private val UNPACK = Module("unpack")
        private val CLIENT = Module("client", setOf(GL, SIGNLINK))
        private val LOADER = Module("loader", setOf(SIGNLINK, UNPACK))
        private val UNPACKCLASS = Module("unpackclass", setOf(UNPACK))

        val ALL = setOf(CLIENT, GL, LOADER, SIGNLINK, UNPACK, UNPACKCLASS)
    }
}
