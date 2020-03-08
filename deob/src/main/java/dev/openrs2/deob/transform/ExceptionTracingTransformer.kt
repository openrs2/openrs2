package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.nextReal
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class ExceptionTracingTransformer : Transformer() {
    private var tracingTryCatches = 0

    override fun preTransform(classPath: ClassPath) {
        tracingTryCatches = 0
    }

    override fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        for (match in CATCH_MATCHER.match(method)) {
            val foundTryCatch = method.tryCatchBlocks.removeIf { tryCatch ->
                tryCatch.type == "java/lang/RuntimeException" && tryCatch.handler.nextReal === match[0]
            }

            if (foundTryCatch) {
                match.forEach(method.instructions::remove)
                tracingTryCatches++
            }
        }
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $tracingTryCatches tracing try/catch blocks" }
    }

    companion object {
        private val logger = InlineLogger()
        private val CATCH_MATCHER = InsnMatcher.compile(
            """
            (ASTORE ALOAD)?
            (LDC INVOKESTATIC |
                NEW DUP
                (LDC INVOKESPECIAL | INVOKESPECIAL LDC INVOKEVIRTUAL)
                ((ILOAD | LLOAD | FLOAD | DLOAD | (ALOAD IFNULL LDC GOTO LDC) | BIPUSH | SIPUSH | LDC) INVOKEVIRTUAL)*
                INVOKEVIRTUAL INVOKESTATIC
            )?
            ATHROW
        """
        )
    }
}
