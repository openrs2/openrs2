package dev.openrs2.decompiler

import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences
import java.io.Closeable
import java.nio.file.Path

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

    private companion object {
        private val OPTIONS = mapOf(
            IFernflowerPreferences.INDENT_STRING to "\t",
            IFernflowerPreferences.SYNTHETIC_NOT_SET to "1"
        )
    }
}
