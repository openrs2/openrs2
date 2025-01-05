package org.openrs2.deob.ast

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.deob.ast.transform.Transformer
import org.openrs2.deob.util.Module

@Singleton
public class AstDeobfuscator @Inject constructor(
    private val transformers: Set<Transformer>,
    private val modules: Set<Module>
) {
    public fun run() {
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
