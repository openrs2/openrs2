package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class CanvasTransformer : Transformer() {
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
