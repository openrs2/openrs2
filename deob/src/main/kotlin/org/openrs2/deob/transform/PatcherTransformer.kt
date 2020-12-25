package org.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.transform.Transformer
import org.openrs2.patcher.PatcherQualifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class PatcherTransformer @Inject constructor(
    @PatcherQualifier private val transformers: Set<Transformer>
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
