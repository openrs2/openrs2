package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class MacResizeTransformer : Transformer() {
    private var branchesRemoved = 0

    override fun preTransform(classPath: ClassPath) {
        branchesRemoved = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        DETECT_MAC_MATCHER.match(method).forEach {
            val getstatic = it[0] as FieldInsnNode
            if (getstatic.owner == "loader" || getstatic.owner == clazz.name || getstatic.desc != "Ljava/lang/String;") {
                return@forEach
            }

            val ldc = it[1] as LdcInsnNode
            if (ldc.cst != "mac") {
                return@forEach
            }

            val invokevirtual = it[2] as MethodInsnNode
            if (invokevirtual.owner != "java/lang/String" || invokevirtual.name != "startsWith" || invokevirtual.desc != "(Ljava/lang/String;)Z") {
                return@forEach
            }

            method.instructions.remove(getstatic)
            method.instructions.remove(ldc)
            method.instructions.remove(invokevirtual)

            val branch = it[3] as JumpInsnNode
            if (branch.opcode == Opcodes.IFEQ) {
                branch.opcode = Opcodes.GOTO
            } else {
                method.instructions.remove(branch)
            }

            branchesRemoved++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $branchesRemoved branches to macOS-specific resize logic" }
    }

    companion object {
        private val logger = InlineLogger()
        private val DETECT_MAC_MATCHER = InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL (IFEQ | IFNE)")
    }
}
