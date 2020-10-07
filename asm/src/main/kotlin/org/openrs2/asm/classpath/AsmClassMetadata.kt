package org.openrs2.asm.classpath

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.openrs2.asm.MemberDesc

public class AsmClassMetadata(
    private val classPath: ClassPath,
    private val clazz: ClassNode,
    override val dependency: Boolean
) : ClassMetadata() {
    override val name: String
        get() = clazz.name

    override val `interface`: Boolean
        get() = clazz.access and Opcodes.ACC_INTERFACE != 0

    override val superClass: ClassMetadata?
        get() = clazz.superName?.let { classPath[it] ?: error("Failed to find $it on provided classpath.") }

    override val superInterfaces: List<ClassMetadata>
        get() = clazz.interfaces.map { classPath[it] ?: error("Failed to find $it on provided classpath.") }

    override val fields: List<MemberDesc>
        get() = clazz.fields.map(::MemberDesc)

    override val methods: List<MemberDesc>
        get() = clazz.methods.map(::MemberDesc)

    override fun getFieldAccess(field: MemberDesc): Int? {
        return clazz.fields.find { it.name == field.name && it.desc == field.desc }?.access
    }

    override fun getMethodAccess(method: MemberDesc): Int? {
        return clazz.methods.find { it.name == method.name && it.desc == method.desc }?.access
    }
}
