package dev.openrs2.deob.ast.gl

import javax.inject.Provider

class GlRegistryProvider : Provider<GlRegistry> {
    override fun get(): GlRegistry {
        GlRegistryProvider::class.java.getResourceAsStream(PATH).use { input ->
            return GlRegistry.parse(input)
        }
    }

    companion object {
        private const val PATH = "/dev/openrs2/deob/ast/gl.xml"
    }
}
