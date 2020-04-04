package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

class InvokeSpecialTransformer : Transformer() {
    private var invokeSpecialsReplaced = 0

    override fun preTransform(classPath: ClassPath) {
        invokeSpecialsReplaced = 0
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        require(clazz.access and (Opcodes.ACC_SUPER or Opcodes.ACC_INTERFACE) != 0)
        return false
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if ((clazz.access and Opcodes.ACC_FINAL) == 0) {
            return false
        }

        for (insn in method.instructions) {
            if (insn !is MethodInsnNode || insn.opcode != Opcodes.INVOKESPECIAL) {
                continue
            } else if (insn.name == "<init>") {
                continue
            } else if (insn.owner != clazz.name) {
                continue
            }

            insn.opcode = Opcodes.INVOKEVIRTUAL
            invokeSpecialsReplaced++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $invokeSpecialsReplaced INVOKESPECIALs with INVOKEVIRTUAL" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
