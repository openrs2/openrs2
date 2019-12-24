package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.deleteSimpleExpression
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class DummyLocalTransformer : Transformer() {
    private var localsRemoved = 0

    override fun preTransform(classPath: ClassPath) {
        localsRemoved = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        /*
         * XXX(gpe): this is primitive (ideally we'd do a proper data flow
         * analysis, but we'd need to do it in reverse and ASM only supports
         * forward data flow), however, it seems to be good enough to catch
         * most dummy locals.
         */
        val loads = BooleanArray(method.maxLocals)

        for (insn in method.instructions) {
            if (insn is VarInsnNode && insn.opcode == Opcodes.ILOAD) {
                loads[insn.`var`] = true
            }
        }

        for (insn in method.instructions) {
            if (insn !is VarInsnNode || insn.opcode != Opcodes.ISTORE) {
                continue
            }

            if (loads[insn.`var`]) {
                continue
            }

            if (method.instructions.deleteSimpleExpression(insn)) {
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
