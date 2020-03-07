package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.removeDeadCode
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.OriginalPcTable
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode

class OriginalPcRestoreTransformer : Transformer() {
    private var originalPcsRestored = 0

    override fun preTransform(classPath: ClassPath) {
        originalPcsRestored = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        method.removeDeadCode(clazz.name)

        val pcs = mutableMapOf<LabelNode, Int>()

        for (insn in method.instructions) {
            if (insn.opcode == -1) {
                continue
            }

            val originalPc = classPath.originalPcs[insn] ?: continue

            val label = LabelNode()
            method.instructions.insertBefore(insn, label)

            pcs[label] = originalPc

            originalPcsRestored++
        }

        if (method.attrs == null) {
            method.attrs = mutableListOf()
        }
        method.attrs.add(OriginalPcTable(pcs))

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Restored $originalPcsRestored original instruction indexes" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
