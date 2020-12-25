package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class LoadLibraryTransformer : Transformer() {
    private var jnilibs = 0
    private var amd64Checks = 0
    private var sunOsChecks = 0

    override fun preTransform(classPath: ClassPath) {
        jnilibs = 0
        amd64Checks = 0
        sunOsChecks = 0
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

        for (match in SUNOS_CHECK_MATCHER.match(method)) {
            val ldc = match[1] as LdcInsnNode
            if (ldc.cst == "sunos") {
                method.instructions.remove(match[0])
                method.instructions.remove(match[1])
                method.instructions.remove(match[2])

                val jump = match[3] as JumpInsnNode
                if (jump.opcode == Opcodes.IFEQ) {
                    method.instructions.set(jump, JumpInsnNode(Opcodes.GOTO, jump.label))
                } else {
                    method.instructions.remove(jump)
                }
                sunOsChecks++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $jnilibs jnilibs with dylibs" }
        logger.info { "Removed $amd64Checks amd64 jagmisc and $sunOsChecks SunOS checks" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val AMD64_CHECK_MATCHER =
            InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL IFNE GETSTATIC LDC INVOKEVIRTUAL IFNE")
        private val SUNOS_CHECK_MATCHER = InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL (IFNE | IFEQ)")
    }
}
