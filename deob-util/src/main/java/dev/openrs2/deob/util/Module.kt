package dev.openrs2.deob.util

import java.nio.file.Path
import java.nio.file.Paths

class Module(val name: String, val dependencies: Set<Module> = emptySet()) {
    val jar: Path = Paths.get("nonfree/var/cache/deob").resolve("$name.jar")
    val sources: Path = Paths.get("nonfree").resolve(name).resolve("src/main/java")
    val transitiveDependencies: Set<Module> = dependencies.plus(dependencies.flatMap { it.transitiveDependencies })

    companion object {
        private val gl = Module("gl")
        private val signLink = Module("signlink")
        private val unpack = Module("unpack")
        private val client = Module("client", setOf(gl, signLink))
        private val loader = Module("loader", setOf(signLink, unpack))
        private val unpackClass = Module("unpackclass", setOf(unpack))

        val all = setOf(client, gl, loader, signLink, unpack, unpackClass)
    }
}
