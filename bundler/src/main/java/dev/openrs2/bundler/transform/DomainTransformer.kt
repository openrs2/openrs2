package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.conf.Config
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DomainTransformer @Inject constructor(
    private val config: Config
) : Transformer() {
    private var domains = 0

    override fun preTransform(classPath: ClassPath) {
        domains = 0
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

            insn.cst = cst.replace("runescape.com", config.domain)
            if (insn.cst != cst) {
                domains++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $domains domains" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
