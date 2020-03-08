package dev.openrs2.deob.transform

import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.transform.Transformer
import dev.openrs2.deob.annotation.OriginalArg
import dev.openrs2.deob.annotation.OriginalClass
import dev.openrs2.deob.annotation.OriginalMember
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodNode

class OriginalNameTransformer : Transformer() {
    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        if (clazz.invisibleAnnotations == null) {
            clazz.invisibleAnnotations = mutableListOf()
        }
        clazz.invisibleAnnotations.add(createOriginalClassAnnotation(clazz.name))
        return false
    }

    override fun transformField(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        field: FieldNode
    ): Boolean {
        if (field.invisibleAnnotations == null) {
            field.invisibleAnnotations = mutableListOf()
        }
        field.invisibleAnnotations.add(createOriginalMemberAnnotation(clazz.name, field.name, field.desc))
        return false
    }

    override fun preTransformMethod(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        if (method.name == "<clinit>") {
            return false
        }

        if (method.invisibleAnnotations == null) {
            method.invisibleAnnotations = mutableListOf()
        }
        method.invisibleAnnotations.add(createOriginalMemberAnnotation(clazz.name, method.name, method.desc))

        val args = Type.getArgumentTypes(method.desc).size
        if (method.invisibleParameterAnnotations == null) {
            method.invisibleParameterAnnotations = arrayOfNulls(args)
        }
        for (i in method.invisibleParameterAnnotations.indices) {
            var annotations = method.invisibleParameterAnnotations[i]
            if (annotations == null) {
                annotations = mutableListOf()
                method.invisibleParameterAnnotations[i] = annotations
            }
            annotations.add(createOriginalArgAnnotation(i))
        }

        return false
    }

    companion object {
        private fun createOriginalClassAnnotation(name: String): AnnotationNode {
            val annotation = AnnotationNode(Type.getDescriptor(OriginalClass::class.java))
            annotation.values = listOf("value", name)
            return annotation
        }

        private fun createOriginalMemberAnnotation(owner: String, name: String, desc: String): AnnotationNode {
            val annotation = AnnotationNode(Type.getDescriptor(OriginalMember::class.java))
            annotation.values = listOf(
                "owner", owner,
                "name", name,
                "descriptor", desc
            )
            return annotation
        }

        private fun createOriginalArgAnnotation(index: Int): AnnotationNode {
            val annotation = AnnotationNode(Type.getDescriptor(OriginalArg::class.java))
            annotation.values = listOf("value", index)
            return annotation
        }
    }
}
