package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberDesc
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode

class AsmClassMetadata(
    private val classPath: ClassPath,
    private val clazz: ClassNode,
    override val dependency: Boolean
) : ClassMetadata() {
    override val name: String
        get() = clazz.name

    override val `interface`
        get() = clazz.access and Opcodes.ACC_INTERFACE != 0

    override val superClass
        get() = clazz.superName?.let { classPath[it] ?: error("Failed to find $it on provided classpath.") }

    override val superInterfaces
        get() = clazz.interfaces.map { classPath[it] ?: error("Failed to find $it on provided classpath.") }

    override val fields
        get() = clazz.fields.map(::MemberDesc)

    override val methods
        get() = clazz.methods.map(::MemberDesc)

    override fun getAccess(method: MemberDesc): Int? {
        return clazz.methods.find { it.name == method.name && it.desc == method.desc }?.access
    }
}
