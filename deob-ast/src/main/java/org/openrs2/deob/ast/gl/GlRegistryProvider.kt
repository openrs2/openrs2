package org.openrs2.deob.ast.gl

import java.nio.file.Files
import java.nio.file.Paths
import javax.inject.Provider

public class GlRegistryProvider : Provider<GlRegistry> {
    override fun get(): GlRegistry {
        return Files.newInputStream(PATH).use { input ->
            GlRegistry.parse(input)
        }
    }

    private companion object {
        private val PATH = Paths.get("share/deob/gl.xml")
    }
}
