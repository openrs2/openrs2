package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import org.openrs2.conf.Config
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class NameTransformer @Inject constructor(
    private val config: Config
) : Transformer() {
    private var names = 0

    override fun preTransform(classPath: ClassPath) {
        names = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn !is LdcInsnNode) {
                continue
            }

            val cst = insn.cst
            if (cst !is String) {
                continue
            }

            insn.cst = cst.replace("RuneScape", config.game)
                .replace("Jagex", config.operator)
            if (insn.cst != cst) {
                names++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $names operator and game names" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
