package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.transform.Transformer
import org.openrs2.patcher.PatcherQualifier

@Singleton
public class PatcherTransformer @Inject constructor(
    @param:PatcherQualifier private val transformers: Set<Transformer>
) : Transformer() {
    override fun transform(classPath: ClassPath) {
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
