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
        get() = if (clazz.superName != null) classPath[clazz.superName] else null

    override val superInterfaces
        get() = clazz.interfaces.map { classPath[it] }.toList()

    override val fields
        get() = clazz.fields.map { MemberDesc(it) }.toList()

    override val methods
        get() = clazz.methods.map { MemberDesc(it) }.toList()

    override fun isNative(method: MemberDesc): Boolean {
        val m = clazz.methods.find { it.name == method.name && it.desc == method.desc } ?: return false
        return m.access and Opcodes.ACC_NATIVE != 0
    }
}
