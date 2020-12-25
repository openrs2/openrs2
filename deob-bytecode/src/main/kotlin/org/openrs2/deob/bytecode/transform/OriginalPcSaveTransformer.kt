package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class OriginalPcSaveTransformer : Transformer() {
    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for ((pc, insn) in method.instructions.filter { it.opcode != -1 }.withIndex()) {
            classPath.originalPcs[insn] = pc
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Saved ${classPath.originalPcs.size} original instruction indexes" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
