package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.bundler.BundlerQualifier
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BundlerTransformer @Inject constructor(
    @BundlerQualifier private val transformers: Set<@JvmSuppressWildcards Transformer>
) : Transformer() {
    override fun transform(classPath: ClassPath) {
        for (transformer in transformers) {
            logger.info { "Running transformer ${transformer.javaClass.simpleName}" }
            transformer.transform(classPath)
        }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
