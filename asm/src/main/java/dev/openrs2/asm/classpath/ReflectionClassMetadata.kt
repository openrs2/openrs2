package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.toInternalClassName
import org.objectweb.asm.Type

private val Class<*>.asmName: String
    get() = name.toInternalClassName()

public class ReflectionClassMetadata(private val classPath: ClassPath, private val clazz: Class<*>) : ClassMetadata() {
    override val name: String
        get() = clazz.asmName

    override val dependency: Boolean
        get() = true

    override val `interface`: Boolean
        get() = clazz.isInterface

    override val superClass: ClassMetadata?
        get() = if (clazz.superclass != null) classPath[clazz.superclass.asmName]!! else null

    override val superInterfaces: List<ClassMetadata>
        get() = clazz.interfaces.map { classPath[it.asmName]!! }

    override val fields: List<MemberDesc>
        get() = clazz.declaredFields.map { MemberDesc(it.name, Type.getDescriptor(it.type)) }

    override val methods: List<MemberDesc>
        get() = clazz.declaredMethods.map { MemberDesc(it.name, Type.getMethodDescriptor(it)) }

    override fun getFieldAccess(field: MemberDesc): Int? {
        return clazz.declaredFields.find { it.name == field.name && Type.getDescriptor(it.type) == field.desc }
            ?.modifiers
    }

    override fun getMethodAccess(method: MemberDesc): Int? {
        return clazz.declaredMethods.find { it.name == method.name && Type.getMethodDescriptor(it) == method.desc }
            ?.modifiers
    }
}
