package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.nextReal
import org.openrs2.asm.transform.Transformer

/**
 * A [Transformer] responsible for removing two kinds of redundant exception
 * handler.
 *
 * - [ZKM](http://www.zelix.com/klassmaster/)'s
 *   [exception obfuscation](https://www.zelix.com/klassmaster/featuresExceptionObfuscation.html),
 *   which inserts exception handlers that catch [RuntimeException]s and
 *   immediately re-throw them. The exception handlers are inserted in
 *   locations where there is no Java source code equivalent, confusing
 *   decompilers.
 *
 * - Jagex inserts a try/catch block around every method that catches
 *   [RuntimeException]s, wraps them with a custom [RuntimeException]
 *   implementation and re-throws them. The wrapped exception's message
 *   contains the values of the method's arguments. While this is for debugging
 *   and not obfuscation, it is clearly automatically-generated and thus we
 *   remove these exception handlers too.
 */
@Singleton
public class ExceptionTracingTransformer : Transformer() {
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

    private companion object {
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
