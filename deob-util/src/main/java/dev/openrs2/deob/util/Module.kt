package dev.openrs2.deob.util

import java.nio.file.Path
import java.nio.file.Paths

class Module(val name: String, val dependencies: Set<Module> = emptySet()) {
    val jar: Path = Paths.get("nonfree/var/cache/deob").resolve("$name.jar")
    val sources: Path = Paths.get("nonfree").resolve(name).resolve("src/main/java")
    val transitiveDependencies: Set<Module> = dependencies.plus(dependencies.flatMap { it.transitiveDependencies })

    companion object {
        private val gl = Module("gl")
        private val signlink = Module("signlink")
        private val unpack = Module("unpack")
        private val client = Module("client", setOf(gl, signlink))
        private val loader = Module("loader", setOf(signlink, unpack))
        private val unpackClass = Module("unpackclass", setOf(unpack))

        val all = setOf(client, gl, loader, signlink, unpack, unpackClass)
    }
}
