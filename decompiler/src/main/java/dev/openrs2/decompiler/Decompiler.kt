package dev.openrs2.decompiler

import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import java.io.Closeable
import java.nio.file.Path
import java.nio.file.Paths

private fun getDestination(archive: String): Path {
    var dir = archive.replace(Regex("(?:_gl)?[.]jar$"), "")
    when (dir) {
        "runescape" -> dir = "client"
        "jaggl" -> dir = "gl"
        "jaggl_dri" -> dir = "gl-dri"
    }
    return Paths.get("nonfree").resolve(dir).resolve("src/main/java")
}

fun main() {
    val deobOutput = Paths.get("nonfree/code/deob")
    val sources = listOf(
        deobOutput.resolve("runescape_gl.jar"),
        deobOutput.resolve("jaggl.jar"),
        deobOutput.resolve("jaggl_dri.jar"),
        deobOutput.resolve("loader_gl.jar"),
        deobOutput.resolve("signlink_gl.jar"),
        deobOutput.resolve("unpack_gl.jar"),
        deobOutput.resolve("unpacker_gl.jar")
    )
    Decompiler(sources, ::getDestination).use {
        it.run()
    }
}

class Decompiler(
    private val sources: List<Path>,
    destination: (String) -> Path
) : Closeable {
    private val io = DecompilerIo(destination)
    private val fernflower = Fernflower(io, io, OPTIONS, Slf4jFernflowerLogger)

    fun run() {
        for (source in sources) {
            fernflower.addSource(source.toFile())
        }
        fernflower.decompileContext()
    }

    override fun close() {
        io.close()
    }

    companion object {
        private val OPTIONS = mapOf(
            IFernflowerPreferences.INDENT_STRING to "\t",
            IFernflowerPreferences.SYNTHETIC_NOT_SET to "1"
        )
    }
}
