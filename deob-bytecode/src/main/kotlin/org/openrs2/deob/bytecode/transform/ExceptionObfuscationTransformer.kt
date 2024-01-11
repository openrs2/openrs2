package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.nextReal
import org.openrs2.asm.transform.Transformer

/**
 * A [Transformer] responsible for removing [ZKM](http://www.zelix.com/klassmaster/)'s
 * [exception obfuscation](https://www.zelix.com/klassmaster/featuresExceptionObfuscation.html),
 * which inserts exception handlers that catch any type of exception and
 * immediately re-throw them. The exception handlers are inserted in locations
 * where there is no Java source code equivalent, confusing decompilers.
 */
@Singleton
public class ExceptionObfuscationTransformer : Transformer() {
    private var handlers = 0

    override fun preTransform(classPath: ClassPath) {
        handlers = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn.opcode != Opcodes.ATHROW) {
                continue
            }

            val foundTryCatch = method.tryCatchBlocks.removeIf { tryCatch ->
                tryCatch.handler.nextReal === insn
            }

            if (foundTryCatch) {
                method.instructions.remove(insn)
                handlers++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $handlers exception obfuscation handlers" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
