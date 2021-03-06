package org.openrs2.deob.bytecode.filter

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.ClassForNameUtils
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.asm.hasCode

public class ReflectedConstructorFilter private constructor(private val classes: Set<String>) : MemberFilter {
    override fun matches(owner: String, name: String, desc: String): Boolean {
        return classes.contains(owner) && name == "<init>"
    }

    public companion object {
        private val logger = InlineLogger()

        public fun create(classPath: ClassPath): MemberFilter {
            val classes = mutableSetOf<String>()

            for (library in classPath.libraries) {
                for (clazz in library) {
                    for (method in clazz.methods) {
                        processMethod(classPath, method, classes)
                    }
                }
            }

            logger.info { "Identified constructors invoked with reflection $classes" }

            return ReflectedConstructorFilter(classes)
        }

        private fun processMethod(classPath: ClassPath, method: MethodNode, classes: MutableSet<String>) {
            if (!method.hasCode) {
                return
            }

            for (name in ClassForNameUtils.findClassNames(method)) {
                val clazz = classPath[name]
                if (clazz != null && !clazz.dependency) {
                    classes.add(name)
                }
            }
        }
    }
}
