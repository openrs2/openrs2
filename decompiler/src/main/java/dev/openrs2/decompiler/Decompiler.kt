package dev.openrs2.decompiler

import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences

class Decompiler(private vararg val libraries: Library) {
    fun run() {
        for (library in libraries) {
            DecompilerIo(library.destination).use { io ->
                val fernflower = Fernflower(io, io, OPTIONS, Slf4jFernflowerLogger)

                for (dependency in library.dependencies) {
                    fernflower.addLibrary(dependency.toFile())
                }
                fernflower.addSource(library.source.toFile())

                fernflower.decompileContext()
            }
        }
    }

    private companion object {
        private val OPTIONS = mapOf(
            IFernflowerPreferences.INDENT_STRING to "\t",
            IFernflowerPreferences.SYNTHETIC_NOT_SET to "1"
        )
    }
}
