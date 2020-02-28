package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class FinalTransformer : Transformer() {
    private var redundantFinals = 0

    override fun preTransform(classPath: ClassPath) {
        redundantFinals = 0
    }

    override fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        if (method.access and Opcodes.ACC_FINAL == 0) {
            return false
        }

        if (method.access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC) != 0) {
            method.access = method.access and Opcodes.ACC_FINAL.inv()
            redundantFinals++
        }
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $redundantFinals redundant final modifiers" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
