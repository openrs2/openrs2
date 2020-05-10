package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Singleton

@Singleton
class HostCheckTransformer : Transformer() {
    private var hostChecks = 0

    override fun preTransform(classPath: ClassPath) {
        hostChecks = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if (Type.getReturnType(method.desc).sort != Type.BOOLEAN) {
            return false
        }

        for (match in GET_HOST_MATCHER.match(method)) {
            val insn1 = match[0] as MethodInsnNode
            if (insn1.owner != clazz.name || insn1.name != "getDocumentBase" || insn1.desc != "()Ljava/net/URL;") {
                continue
            }

            val insn2 = match[1] as MethodInsnNode
            if (insn2.owner != "java/net/URL" || insn2.name != "getHost" || insn2.desc != "()Ljava/lang/String;") {
                continue
            }

            val insn3 = match[2] as MethodInsnNode
            if (
                insn3.owner != "java/lang/String" ||
                insn3.name != "toLowerCase" ||
                insn3.desc != "()Ljava/lang/String;"
            ) {
                continue
            }

            method.instructions.clear()
            method.tryCatchBlocks.clear()

            method.instructions.add(InsnNode(Opcodes.ICONST_1))
            method.instructions.add(InsnNode(Opcodes.IRETURN))

            hostChecks++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $hostChecks host checks" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val GET_HOST_MATCHER = InsnMatcher.compile("INVOKEVIRTUAL INVOKEVIRTUAL INVOKEVIRTUAL")
    }
}
