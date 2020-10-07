package org.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.nextReal
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class FernflowerExceptionTransformer : Transformer() {
    private var nopsInserted = 0

    override fun preTransform(classPath: ClassPath) {
        nopsInserted = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if (method.tryCatchBlocks.any { it.end.nextReal == null }) {
            method.instructions.add(InsnNode(Opcodes.NOP))
            nopsInserted++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Inserted $nopsInserted NOPs to correct Fernflower's exception generation" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
