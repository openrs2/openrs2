package dev.openrs2.bundler

import dev.openrs2.bundler.transform.*

class Bundler {
    companion object {
        @JvmField
        val TRANSFORMERS = listOf(
            BufferSizeTransformer(),
            CachePathTransformer(),
            HostCheckTransformer(),
            MacResizeTransformer(),
            RightClickTransformer(),
            LoadLibraryTransformer()
        )
    }
}
