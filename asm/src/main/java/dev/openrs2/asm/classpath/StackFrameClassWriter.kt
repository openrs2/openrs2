package dev.openrs2.asm.classpath

import org.objectweb.asm.ClassWriter

public class StackFrameClassWriter(private val classPath: ClassPath) : ClassWriter(COMPUTE_FRAMES) {
    override fun getCommonSuperClass(type1: String, type2: String): String {
        var c = classPath[type1]!!
        val d = classPath[type2]!!
        return when {
            c.isAssignableFrom(d) -> type1
            d.isAssignableFrom(c) -> type2
            c.`interface` || d.`interface` -> "java/lang/Object"
            else -> {
                do {
                    c = c.superClass!!
                } while (!c.isAssignableFrom(d))
                c.name
            }
        }
    }
}
