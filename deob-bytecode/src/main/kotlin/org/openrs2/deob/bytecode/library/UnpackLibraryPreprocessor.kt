package org.openrs2.deob.bytecode.library

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.openrs2.asm.classpath.Library
import org.openrs2.deob.util.module.Module
import org.openrs2.deob.util.module.ModuleType
import org.openrs2.deob.util.module.ModuleType.LOADER
import org.openrs2.deob.util.module.ModuleType.UNPACK

@Singleton
public class UnpackLibraryPreprocessor : LibraryPreprocessor(LOADER, UNPACK) {
    override fun preprocess(modules: Map<ModuleType, Module>, libraries: Map<ModuleType, Library>) {
        // move unpack class out of the loader (so the unpacker and loader can both depend on it)
        logger.info { "Moving unpack from loader to unpack" }

        val loader = libraries[LOADER]!!
        libraries[UNPACK]!!.add(loader.remove("unpack")!!)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
