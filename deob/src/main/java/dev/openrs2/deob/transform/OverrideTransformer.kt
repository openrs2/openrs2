package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import javax.inject.Singleton

@Singleton
class OverrideTransformer : Transformer() {
    private var overrides = 0

    override fun preTransform(classPath: ClassPath) {
        overrides = 0
    }

    override fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        if (method.name == "<init>" || method.name == "<clinit>" || method.access and Opcodes.ACC_STATIC != 0) {
            return false
        }

        if (!classPath[clazz.name]!!.isOverride(MemberDesc(method))) {
            return false
        }

        if (method.invisibleAnnotations != null && method.invisibleAnnotations.any { it.desc == OVERRIDE_DESC }) {
            return false
        }

        if (method.invisibleAnnotations == null) {
            method.invisibleAnnotations = mutableListOf()
        }
        method.invisibleAnnotations.add(AnnotationNode(OVERRIDE_DESC))
        overrides++

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Added $overrides override annotations" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val OVERRIDE_DESC: String = Type.getDescriptor(Override::class.java)
    }
}
