package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer

@Singleton
public class FinalClassTransformer : Transformer() {
    private val superClasses = mutableListOf<String>()

    override fun preTransform(classPath: ClassPath) {
        superClasses.clear()
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        val superClass = clazz.superName
        if (superClass != null) {
            superClasses += superClass
        }

        superClasses.addAll(clazz.interfaces)

        return false
    }

    private fun isClassFinal(clazz: ClassNode): Boolean {
        if ((clazz.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE)) != 0) {
            return false
        }

        return !superClasses.contains(clazz.name)
    }

    override fun postTransform(classPath: ClassPath) {
        var classesChanged = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                val access = clazz.access

                if (isClassFinal(clazz)) {
                    clazz.access = access or Opcodes.ACC_FINAL
                } else {
                    clazz.access = access and Opcodes.ACC_FINAL.inv()
                }

                if (clazz.access != access) {
                    classesChanged++
                }
            }
        }

        logger.info { "Updated final modifier on $classesChanged classes" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
