package org.openrs2.deob.ast

import com.github.michaelbull.logging.InlineLogger
import org.openrs2.deob.ast.transform.Transformer
import org.openrs2.deob.util.Module
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class AstDeobfuscator @Inject constructor(
    private val transformers: Set<Transformer>
) {
    public fun run(modules: Set<Module>) {
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
