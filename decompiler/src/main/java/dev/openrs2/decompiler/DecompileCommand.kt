package dev.openrs2.decompiler

import com.github.ajalt.clikt.core.CliktCommand
import java.nio.file.Path
import java.nio.file.Paths

fun main(args: Array<String>) = DecompileCommand().main(args)

class DecompileCommand : CliktCommand(name = "decompile") {
    override fun run() {
        val deobOutput = Paths.get("nonfree/var/cache/deob")

        val client = deobOutput.resolve("runescape_gl.jar")
        val gl = deobOutput.resolve("jaggl.jar")
        val loader = deobOutput.resolve("loader_gl.jar")
        val signlink = deobOutput.resolve("signlink_gl.jar")
        val unpack = deobOutput.resolve("unpack_gl.jar")
        val unpackClass = deobOutput.resolve("unpackclass_gl.jar")

        val decompiler = Decompiler(
            Library(
                source = client,
                destination = getDestination("client"),
                dependencies = listOf(gl, signlink)
            ),
            Library(
                source = gl,
                destination = getDestination("gl")
            ),
            Library(
                source = loader,
                destination = getDestination("loader"),
                dependencies = listOf(signlink, unpack)
            ),
            Library(
                source = signlink,
                destination = getDestination("signlink")
            ),
            Library(
                source = unpack,
                destination = getDestination("unpack")
            ),
            Library(
                source = unpackClass,
                destination = getDestination("unpackclass"),
                dependencies = listOf(unpack)
            )
        )
        decompiler.run()
    }

    private fun getDestination(dir: String): Path {
        return Paths.get("nonfree").resolve(dir).resolve("src/main/java")
    }
}
