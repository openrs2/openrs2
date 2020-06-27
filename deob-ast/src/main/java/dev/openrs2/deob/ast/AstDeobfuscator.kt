package dev.openrs2.deob.ast

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.deob.ast.transform.Transformer
import dev.openrs2.deob.util.Module
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AstDeobfuscator @Inject constructor(
    private val transformers: Set<@JvmSuppressWildcards Transformer>
) {
    fun run(modules: Set<Module>) {
        val group = LibraryGroup(modules.map(Library.Companion::parse))

        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(group)
        }

        group.forEach(Library::save)
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
