package dev.openrs2.deob.transform

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

class CanvasTransformer : Transformer() {
    override fun transformClass(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode
    ): Boolean {
        if (clazz.superName != "java/awt/Canvas") {
            return false
        }

        if (clazz.access and Opcodes.ACC_FINAL == 0) {
            return false
        }

        clazz.interfaces.remove("java/awt/event/FocusListener")
        return false
    }
}
