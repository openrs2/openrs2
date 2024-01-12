package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.bytecode.remap.StaticFieldUnscrambler

/**
 * A [Transformer] that splits multiple assignments to static fields in a
 * single expression in `<clinit>` methods. For example, `a = b = new X()` is
 * translated to `b = new X(); a = b`. This allows [StaticFieldUnscrambler] to
 * move the fields independently.
 */
@Singleton
public class MultipleAssignmentTransformer : Transformer() {
    private var assignments = 0

    override fun preTransform(classPath: ClassPath) {
        assignments = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if (method.name != "<clinit>") {
            return false
        }

        for (match in MATCHER.match(method)) {
            for (i in 0 until match.size - 1 step 2) {
                val dup = match[i]
                val putstatic = match[i + 1] as FieldInsnNode

                method.instructions.remove(dup)
                method.instructions.insert(putstatic, FieldInsnNode(Opcodes.GETSTATIC, putstatic.owner, putstatic.name, putstatic.desc))

                assignments++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Split $assignments multiple assignment expressions into separate expressions" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val MATCHER = InsnMatcher.compile("(DUP PUTSTATIC)+ PUTSTATIC")
    }
}
