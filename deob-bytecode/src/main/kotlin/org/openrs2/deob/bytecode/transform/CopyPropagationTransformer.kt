package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.bytecode.analysis.CopyPropagationAnalyzer

/**
 * A [Transformer] that performs
 * [copy propagation](https://en.wikipedia.org/wiki/Copy_propagation) of
 * assignments of one variable to another.
 *
 * This is primarily for improving the decompilation of `for` loops. Without
 * copy propagation, the initializer in many `for` loops declares a different
 * variable to the one in the increment expression:
 *
 * ```
 * Object[] array = ...
 * int i = 0;
 * for (Object[] array2 = array; i < n; i++) {
 *     // use array2[n]
 * }
 * ```
 *
 * With copy propagation, the variables match:
 *
 * ```
 * Object[] array = ...
 * for (int i = 0; i < n; i++) {
 *     // use array[n]
 * }
 * ```
 */
@Singleton
public class CopyPropagationTransformer : Transformer() {
    private var propagatedLocals = 0

    override fun preTransform(classPath: ClassPath) {
        propagatedLocals = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val analyzer = CopyPropagationAnalyzer(clazz.name, method)
        analyzer.analyze()

        for (insn in method.instructions) {
            if (insn !is VarInsnNode || !LOAD_OPCODES.contains(insn.opcode)) {
                continue
            }

            val set = analyzer.getInSet(insn) ?: continue
            val assignment = set.singleOrNull { it.destination == insn.`var` } ?: continue
            insn.`var` = assignment.source
            propagatedLocals++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Propagated $propagatedLocals copies" }
    }

    private companion object {
        private val logger = InlineLogger()

        private val LOAD_OPCODES = setOf(
            Opcodes.ILOAD,
            Opcodes.LLOAD,
            Opcodes.FLOAD,
            Opcodes.DLOAD,
            Opcodes.ALOAD
        )
    }
}
