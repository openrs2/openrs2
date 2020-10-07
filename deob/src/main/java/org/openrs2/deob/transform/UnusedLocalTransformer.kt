package org.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.deleteExpression
import org.openrs2.asm.isPure
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.analysis.LiveVariableAnalyzer
import javax.inject.Singleton

@Singleton
public class UnusedLocalTransformer : Transformer() {
    private var localsRemoved = 0

    override fun preTransform(classPath: ClassPath) {
        localsRemoved = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val analyzer = LiveVariableAnalyzer(clazz.name, method)
        analyzer.analyze()

        val deadStores = mutableListOf<AbstractInsnNode>()

        for (insn in method.instructions) {
            if (insn !is VarInsnNode || !STORE_OPCODES.contains(insn.opcode)) {
                continue
            }

            val live = analyzer.getInSet(insn)?.contains(insn.`var`) ?: false
            if (live) {
                continue
            }

            deadStores += insn
        }

        for (insn in deadStores) {
            if (method.instructions.deleteExpression(insn, AbstractInsnNode::isPure)) {
                localsRemoved++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $localsRemoved unused local variables" }
    }

    private companion object {
        private val logger = InlineLogger()

        private val STORE_OPCODES = setOf(
            Opcodes.ISTORE,
            Opcodes.LSTORE,
            Opcodes.FSTORE,
            Opcodes.DSTORE,
            Opcodes.ASTORE
        )
    }
}
