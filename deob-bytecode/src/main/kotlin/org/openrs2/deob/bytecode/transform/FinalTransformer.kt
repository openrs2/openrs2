package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import org.openrs2.util.collect.DisjointSet
import javax.inject.Singleton

@Singleton
public class FinalTransformer : Transformer() {
    private val superClasses = mutableListOf<String>()
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private var methodsChanged = 0

    override fun preTransform(classPath: ClassPath) {
        superClasses.clear()
        inheritedMethodSets = classPath.createInheritedMethodSets()
        methodsChanged = 0
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        val superClass = clazz.superName
        if (superClass != null) {
            superClasses += superClass
        }

        superClasses.addAll(clazz.interfaces)

        return false
    }

    private fun isMethodFinal(classPath: ClassPath, clazz: ClassNode, method: MethodNode): Boolean {
        if (method.name == "<init>") {
            return false
        } else if ((method.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_PRIVATE or Opcodes.ACC_STATIC)) != 0) {
            return false
        }

        val thisClass = classPath[clazz.name]!!

        val partition = inheritedMethodSets[MemberRef(clazz, method)]!!
        for (methodRef in partition) {
            if (methodRef.owner == clazz.name) {
                continue
            }

            val otherClass = classPath[methodRef.owner]!!
            if (otherClass.methods.none { it.name == methodRef.name && it.desc == methodRef.desc }) {
                continue
            }

            if (thisClass.isAssignableFrom(otherClass)) {
                return false
            }
        }

        return true
    }

    override fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        val access = method.access

        if (isMethodFinal(classPath, clazz, method)) {
            method.access = access or Opcodes.ACC_FINAL
        } else {
            method.access = access and Opcodes.ACC_FINAL.inv()
        }

        if (method.access != access) {
            methodsChanged++
        }

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

        logger.info { "Updated final modifier on $classesChanged classes and $methodsChanged methods" }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
