package org.openrs2.deob.ast.gl

import jakarta.inject.Provider
import java.nio.file.Files
import java.nio.file.Path

public class GlRegistryProvider : Provider<GlRegistry> {
    override fun get(): GlRegistry {
        return Files.newInputStream(PATH).use { input ->
            GlRegistry.parse(input)
        }
    }

    private companion object {
        private val PATH = Path.of("share/deob/gl.xml")
    }
}
