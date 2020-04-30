package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Singleton

@Singleton
class LoadLibraryTransformer : Transformer() {
    private var jnilibs = 0
    private var amd64Checks = 0

    override fun preTransform(classPath: ClassPath) {
        jnilibs = 0
        amd64Checks = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val jnilib = method.instructions.find { it is LdcInsnNode && it.cst == "libjaggl.jnilib" } ?: return false
        jnilib as LdcInsnNode
        jnilib.cst = "libjaggl.dylib"
        jnilibs++

        for (match in AMD64_CHECK_MATCHER.match(method)) {
            val ldc = match[1] as LdcInsnNode
            if (ldc.cst == "amd64") {
                match.forEach(method.instructions::remove)
                amd64Checks++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $jnilibs jnilibs with dylibs and removed $amd64Checks amd64 jagmisc checks" }
    }

    companion object {
        private val logger = InlineLogger()
        private val AMD64_CHECK_MATCHER =
            InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL IFNE GETSTATIC LDC INVOKEVIRTUAL IFNE")
    }
}
