package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.deleteExpression
import dev.openrs2.asm.pure
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.analysis.LiveVariableAnalyzer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class DummyLocalTransformer : Transformer() {
    private var localsRemoved = 0

    override fun preTransform(classPath: ClassPath) {
        localsRemoved = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val analyzer = LiveVariableAnalyzer(clazz.name, method)
        analyzer.analyze()

        val deadStores = mutableListOf<AbstractInsnNode>()

        for (insn in method.instructions) {
            if (insn !is VarInsnNode || insn.opcode != Opcodes.ISTORE) {
                continue
            }

            val live = analyzer.getInSet(insn)?.contains(insn.`var`) ?: false
            if (live) {
                continue
            }

            deadStores += insn
        }

        for (insn in deadStores) {
            if (method.instructions.deleteExpression(insn, AbstractInsnNode::pure)) {
                localsRemoved++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $localsRemoved dummy local variables" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
