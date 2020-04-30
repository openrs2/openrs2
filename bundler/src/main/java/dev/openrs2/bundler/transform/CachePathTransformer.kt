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
class CachePathTransformer @Inject constructor(
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
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Updated $paths cache paths" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
