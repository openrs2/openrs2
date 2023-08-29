package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer

@Singleton
public class TypoTransformer : Transformer() {
    private var errorsFixed = 0

    override fun preTransform(classPath: ClassPath) {
        errorsFixed = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn !is LdcInsnNode) {
                continue
            }

            if (insn.cst == "Carregando /secure/libs_v4s/RCras - ") {
                insn.cst = "Carregando texturas - "
                errorsFixed++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Fixed $errorsFixed typographical errors" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
