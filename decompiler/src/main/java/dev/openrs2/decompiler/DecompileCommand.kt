package dev.openrs2.decompiler

import com.github.ajalt.clikt.core.CliktCommand
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) = DecompileCommand().main(args)

class DecompileCommand : CliktCommand(name = "decompile") {
    override fun run() {
        val deobOutput = Paths.get("nonfree/var/cache/deob")
        val sources = listOf(
            deobOutput.resolve("runescape_gl.jar"),
            deobOutput.resolve("jaggl.jar"),
            deobOutput.resolve("loader_gl.jar"),
            deobOutput.resolve("signlink_gl.jar"),
            deobOutput.resolve("unpack_gl.jar"),
            deobOutput.resolve("unpackclass_gl.jar")
        )
        Decompiler(sources, ::getDestination).use {
            it.run()
        }
    }

    private fun getDestination(archive: String): Path {
        var dir = archive.replace(JAR_SUFFIX_REGEX, "")
        when (dir) {
            "runescape" -> dir = "client"
            "jaggl" -> dir = "gl"
        }
        return Paths.get("nonfree").resolve(dir).resolve("src/main/java")
    }

    private companion object {
        private val JAR_SUFFIX_REGEX = Regex("(?:_gl)?[.]jar$")
    }
}
