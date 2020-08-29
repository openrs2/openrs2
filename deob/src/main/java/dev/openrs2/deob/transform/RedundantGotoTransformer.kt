package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.nextReal
import dev.openrs2.asm.removeDeadCode
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Singleton

@Singleton
public class RedundantGotoTransformer : Transformer() {
    private var removed = 0

    override fun preTransform(classPath: ClassPath) {
        removed = 0
    }

    override fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        method.removeDeadCode(clazz.name)
        return false
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (instruction in method.instructions) {
            if (instruction.opcode == Opcodes.GOTO) {
                instruction as JumpInsnNode

                if (instruction.nextReal === instruction.label.nextReal) {
                    method.instructions.remove(instruction)
                    removed++
                }
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $removed redundant GOTO instructions" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
