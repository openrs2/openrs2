package dev.openrs2.asm.transform

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.AnalyzerException

abstract class Transformer {
    @Throws(AnalyzerException::class)
    fun transform(classPath: ClassPath) {
        preTransform(classPath)

        var changed: Boolean
        do {
            changed = false

            prePass(classPath)
            for (library in classPath.libraries) {
                for (clazz in library) {
                    changed = changed or transformClass(classPath, library, clazz)

                    for (field in clazz.fields) {
                        changed = changed or transformField(classPath, library, clazz, field)
                    }

                    for (method in clazz.methods) {
                        changed = changed or preTransformMethod(classPath, library, clazz, method)

                        if (method.access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT) == 0) {
                            changed = changed or transformCode(classPath, library, clazz, method)
                        }

                        changed = changed or postTransformMethod(classPath, library, clazz, method)
                    }
                }
            }
            postPass(classPath)
        } while (changed)

        postTransform(classPath)
    }

    @Throws(AnalyzerException::class)
    protected open fun preTransform(classPath: ClassPath) {
        /* empty */
    }

    @Throws(AnalyzerException::class)
    protected open fun prePass(classPath: ClassPath) {
        /* empty */
    }

    @Throws(AnalyzerException::class)
    protected open fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        return false
    }

    @Throws(AnalyzerException::class)
    protected open fun transformField(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        field: FieldNode
    ): Boolean {
        return false
    }

    @Throws(AnalyzerException::class)
    protected open fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        return false
    }

    @Throws(AnalyzerException::class)
    protected open fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        return false
    }

    @Throws(AnalyzerException::class)
    protected open fun postTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        return false
    }

    @Throws(AnalyzerException::class)
    protected open fun postPass(classPath: ClassPath) {
        /* empty */
    }

    @Throws(AnalyzerException::class)
    protected open fun postTransform(classPath: ClassPath) {
        /* empty */
    }
}
