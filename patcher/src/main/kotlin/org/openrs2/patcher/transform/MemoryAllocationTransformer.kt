package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.replaceExpression
import org.openrs2.asm.transform.Transformer

@Singleton
public class MemoryAllocationTransformer : Transformer() {
    private var usedMemoryExprsZeroed = 0

    override fun preTransform(classPath: ClassPath) {
        usedMemoryExprsZeroed = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if ((method.access and Opcodes.ACC_STATIC) != 0) {
            return false
        }

        for (match in USED_MEMORY_MATCHER.match(method)) {
            val load1 = match[0] as VarInsnNode
            val load2 = match[2] as VarInsnNode
            if (load1.`var` != load2.`var`) {
                continue
            }

            val invoke1 = match[1] as MethodInsnNode
            if (invoke1.owner != "java/lang/Runtime" || invoke1.name != "totalMemory" || invoke1.desc != "()J") {
                continue
            }

            val invoke2 = match[3] as MethodInsnNode
            if (invoke2.owner != "java/lang/Runtime" || invoke2.name != "freeMemory" || invoke2.desc != "()J") {
                continue
            }

            if (method.instructions.replaceExpression(match[4], InsnNode(Opcodes.LCONST_0))) {
                usedMemoryExprsZeroed++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Zeroed $usedMemoryExprsZeroed used memory expressions" }
    }

    private companion object {
        private val logger = InlineLogger()

        private val USED_MEMORY_MATCHER = InsnMatcher.compile("ALOAD INVOKEVIRTUAL ALOAD INVOKEVIRTUAL LSUB")
    }
}
