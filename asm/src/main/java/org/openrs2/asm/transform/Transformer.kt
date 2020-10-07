package org.openrs2.asm.transform

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.hasCode

public abstract class Transformer {
    public open fun transform(classPath: ClassPath) {
        preTransform(classPath)

        var changed: Boolean
        do {
            changed = prePass(classPath)

            for (library in classPath.libraries) {
                for (clazz in library) {
                    changed = changed or transformClass(classPath, library, clazz)

                    for (field in clazz.fields) {
                        changed = changed or transformField(classPath, library, clazz, field)
                    }

                    for (method in clazz.methods) {
                        changed = changed or preTransformMethod(classPath, library, clazz, method)

                        if (method.hasCode) {
                            changed = changed or transformCode(classPath, library, clazz, method)
                        }

                        changed = changed or postTransformMethod(classPath, library, clazz, method)
                    }
                }
            }

            changed = changed or postPass(classPath)
        } while (changed)

        postTransform(classPath)
    }

    protected open fun preTransform(classPath: ClassPath) {
        // empty
    }

    protected open fun prePass(classPath: ClassPath): Boolean {
        return false
    }

    protected open fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        return false
    }

    protected open fun transformField(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        field: FieldNode
    ): Boolean {
        return false
    }

    protected open fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        return false
    }

    protected open fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        return false
    }

    protected open fun postTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        return false
    }

    protected open fun postPass(classPath: ClassPath): Boolean {
        return false
    }

    protected open fun postTransform(classPath: ClassPath) {
        // empty
    }
}
