package dev.openrs2.deob.filter

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.ClassForNameUtils
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.filter.MemberFilter
import dev.openrs2.asm.hasCode
import org.objectweb.asm.tree.MethodNode

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
