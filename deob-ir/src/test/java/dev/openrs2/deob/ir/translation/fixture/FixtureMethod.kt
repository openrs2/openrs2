package dev.openrs2.deob.ir.translation.fixture

import org.objectweb.asm.ClassReader
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import java.io.File
import kotlin.reflect.KClass

class FixtureMethod private constructor(val owner: ClassNode, val method: MethodNode) {
    companion object {
        fun from(ty: KClass<out Fixture>): FixtureMethod {
            val classPath = "/${ty.java.name.replace('.', File.separatorChar)}.class"
            val classFile = ty.java.getResourceAsStream(classPath)

            return classFile.use {
                val node = ClassNode()
                val reader = ClassReader(classFile)

                reader.accept(node, ClassReader.SKIP_DEBUG)

                val method = node.methods.find { it.name == "test" } ?:
                    throw IllegalStateException("Fixture class has no test() method")

                return FixtureMethod(node, method)
            }
        }
    }
}
