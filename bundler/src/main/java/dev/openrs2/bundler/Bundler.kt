package dev.openrs2.bundler

import dev.openrs2.bundler.transform.BufferSizeTransformer
import dev.openrs2.bundler.transform.CachePathTransformer
import dev.openrs2.bundler.transform.HostCheckTransformer
import dev.openrs2.bundler.transform.LoadLibraryTransformer
import dev.openrs2.bundler.transform.MacResizeTransformer
import dev.openrs2.bundler.transform.RightClickTransformer

class Bundler {
    companion object {
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
