package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class OriginalPcSaveTransformer : Transformer() {
    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for ((pc, insn) in method.instructions.filter { it.opcode != -1 }.withIndex()) {
            classPath.originalPcs[insn] = pc
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Saved ${classPath.originalPcs.size} original instruction indexes" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
