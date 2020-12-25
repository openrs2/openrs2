package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class MacResizeTransformer : Transformer() {
    private var branchesRemoved = 0

    override fun preTransform(classPath: ClassPath) {
        branchesRemoved = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (match in DETECT_MAC_MATCHER.match(method)) {
            val getstatic = match[0] as FieldInsnNode
            if (
                getstatic.owner == "loader" ||
                getstatic.owner == "loader!loader" ||
                getstatic.owner == clazz.name ||
                getstatic.desc != "Ljava/lang/String;"
            ) {
                continue
            }

            val ldc = match[1] as LdcInsnNode
            if (ldc.cst != "mac") {
                continue
            }

            val invokevirtual = match[2] as MethodInsnNode
            if (
                invokevirtual.owner != "java/lang/String" ||
                invokevirtual.name != "startsWith" ||
                invokevirtual.desc != "(Ljava/lang/String;)Z"
            ) {
                continue
            }

            method.instructions.remove(getstatic)
            method.instructions.remove(ldc)
            method.instructions.remove(invokevirtual)

            val branch = match[3] as JumpInsnNode
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

    private companion object {
        private val logger = InlineLogger()
        private val DETECT_MAC_MATCHER = InsnMatcher.compile("GETSTATIC LDC INVOKEVIRTUAL (IFEQ | IFNE)")
    }
}
