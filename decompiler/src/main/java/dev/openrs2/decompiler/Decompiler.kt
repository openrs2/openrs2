package dev.openrs2.decompiler

import dev.openrs2.deob.util.Module
import org.jetbrains.java.decompiler.main.Fernflower
import org.jetbrains.java.decompiler.main.extern.IFernflowerPreferences

public class Decompiler(private val modules: Set<Module>) {
    public fun run() {
        for (module in modules) {
            DecompilerIo(module.sources).use { io ->
                val fernflower = Fernflower(io, io, OPTIONS, Slf4jFernflowerLogger)

                for (dependency in module.transitiveDependencies) {
                    fernflower.addLibrary(dependency.jar.toFile())
                }
                fernflower.addSource(module.jar.toFile())

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
