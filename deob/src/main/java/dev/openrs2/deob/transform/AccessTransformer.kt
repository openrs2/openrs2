package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class AccessTransformer : Transformer() {
    private var redundantFinals = 0
    private var packagePrivate = 0

    override fun preTransform(classPath: ClassPath) {
        redundantFinals = 0
        packagePrivate = 0
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        if (clazz.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE) == 0) {
            clazz.access = clazz.access or Opcodes.ACC_PUBLIC
            packagePrivate++
        }
        return false
    }

    override fun transformField(classPath: ClassPath, library: Library, clazz: ClassNode, field: FieldNode): Boolean {
        if (field.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE) == 0) {
            field.access = field.access or Opcodes.ACC_PUBLIC
            packagePrivate++
        }
        return false
    }

    override fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        if (method.access and (Opcodes.ACC_PUBLIC or Opcodes.ACC_PROTECTED or Opcodes.ACC_PRIVATE) == 0) {
            method.access = method.access or Opcodes.ACC_PUBLIC
            packagePrivate++
        }

        if (method.access and Opcodes.ACC_FINAL == 0) {
            return false
        }

        if (method.access and (Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC) != 0) {
            method.access = method.access and Opcodes.ACC_FINAL.inv()
            redundantFinals++
        }
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed $redundantFinals redundant final modifiers" }
        logger.info { "Made $packagePrivate package-private classes, fields and methods public" }
    }

    companion object {
        private val logger = InlineLogger()
    }
}
