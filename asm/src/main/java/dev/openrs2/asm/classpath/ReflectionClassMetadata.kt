package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberDesc
import org.objectweb.asm.Type

private val Class<*>.asmName: String
    get() = name.replace('.', '/')

class ReflectionClassMetadata(private val classPath: ClassPath, private val clazz: Class<*>) : ClassMetadata() {
    override val name: String
        get() = clazz.asmName

    override val dependency: Boolean
        get() = true

    override val `interface`: Boolean
        get() = clazz.isInterface

    override val superClass: ClassMetadata?
        get() = if (clazz.superclass != null) classPath[clazz.superclass.asmName] else null

    override val superInterfaces
        get() = clazz.interfaces.map { classPath[it.asmName] }.toList()

    override val fields
        get() = clazz.declaredFields.map { MemberDesc(it.name, Type.getDescriptor(it.type)) }.toList()

    override val methods
        get() = clazz.declaredMethods.map { MemberDesc(it.name, Type.getMethodDescriptor(it)) }.toList()

    override fun getAccess(method: MemberDesc): Int? {
        return clazz.declaredMethods.find { it.name == method.name && Type.getMethodDescriptor(it) == method.desc }
            ?.modifiers
    }
}
