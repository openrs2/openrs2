package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.InsnNodeUtils
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode

class BitShiftTransformer : Transformer() {
    private var simplified = 0

    override fun preTransform(classPath: ClassPath) {
        simplified = 0
    }

    override fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        CONST_SHIFT_MATCHER.match(method).forEach {
            val push = it[0]
            val bits = InsnNodeUtils.getIntConstant(push)

            val opcode = it[1].opcode
            val mask = if (LONG_SHIFTS.contains(opcode)) 63 else 31

            val simplifiedBits = bits and mask

            if (bits != simplifiedBits) {
                method.instructions[push] = InsnNodeUtils.createIntConstant(simplifiedBits)
                simplified++
            }
        }
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Simplified $simplified bit shifts" }
    }

    companion object {
        private val logger = InlineLogger()
        private val CONST_SHIFT_MATCHER =
            InsnMatcher.compile("(ICONST | BIPUSH | SIPUSH | LDC) (ISHL | ISHR | IUSHR | LSHL | LSHR | LUSHR)")
        private val LONG_SHIFTS = setOf(Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR)
    }
}
