package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import org.openrs2.conf.Config

@Singleton
public class CachePathTransformer @Inject constructor(
    private val config: Config
) : Transformer() {
    private var paths = 0

    override fun preTransform(classPath: ClassPath) {
        paths = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn !is LdcInsnNode) {
                continue
            }

            when (insn.cst) {
                ".jagex_cache_", ".file_store_" -> {
                    insn.cst = ".${config.internalOperator}_cache_"
                    paths++
                }

                "jagex_" -> {
                    insn.cst = ".${config.internalOperator}_"
                    paths++
                }

                "runescape" -> {
                    insn.cst = config.internalGame
                    paths++
                }
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Updated $paths cache paths" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
