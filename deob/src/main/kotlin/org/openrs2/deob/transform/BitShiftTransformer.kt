package org.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.intConstant
import org.openrs2.asm.toAbstractInsnNode
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class BitShiftTransformer : Transformer() {
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
        for (match in CONST_SHIFT_MATCHER.match(method)) {
            val push = match[0]
            val bits = push.intConstant!!

            val opcode = match[1].opcode
            val mask = if (opcode in LONG_SHIFTS) 63 else 31

            val simplifiedBits = bits and mask

            if (bits != simplifiedBits) {
                method.instructions[push] = simplifiedBits.toAbstractInsnNode()
                simplified++
            }
        }
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Simplified $simplified bit shifts" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val CONST_SHIFT_MATCHER =
            InsnMatcher.compile("(ICONST | BIPUSH | SIPUSH | LDC) (ISHL | ISHR | IUSHR | LSHL | LSHR | LUSHR)")
        private val LONG_SHIFTS = setOf(Opcodes.LSHL, Opcodes.LSHR, Opcodes.LUSHR)
    }
}
