package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class RightClickTransformer : Transformer() {
    private var metaDownCalls = 0

    override fun preTransform(classPath: ClassPath) {
        metaDownCalls = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn !is MethodInsnNode || insn.opcode != Opcodes.INVOKEVIRTUAL) {
                continue
            }

            if (insn.owner != "java/awt/event/MouseEvent" && insn.owner != "java/awt/event/InputEvent") {
                continue
            }

            if (insn.name != "isMetaDown" || insn.desc != "()Z") {
                continue
            }

            insn.name = "getModifiersEx"
            insn.desc = "()I"

            val list = InsnList()
            list.add(FieldInsnNode(Opcodes.GETSTATIC, "java/awt/event/MouseEvent", "BUTTON3_DOWN_MASK", "I"))
            list.add(InsnNode(Opcodes.IAND))
            method.instructions.insert(insn, list)

            metaDownCalls++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $metaDownCalls isMetaDown calls with getModifiersEx" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
