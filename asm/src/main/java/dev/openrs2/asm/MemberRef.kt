package dev.openrs2.asm

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

data class MemberRef(val owner: String, val name: String, val desc: String) : Comparable<MemberRef> {
    constructor(clazz: ClassNode, field: FieldNode) : this(clazz.name, field.name, field.desc)
    constructor(clazz: ClassNode, method: MethodNode) : this(clazz.name, method.name, method.desc)
    constructor(fieldInsn: FieldInsnNode) : this(fieldInsn.owner, fieldInsn.name, fieldInsn.desc)
    constructor(methodInsn: MethodInsnNode) : this(methodInsn.owner, methodInsn.name, methodInsn.desc)
    constructor(owner: String, desc: MemberDesc) : this(owner, desc.name, desc.desc)

    override fun compareTo(other: MemberRef): Int {
        var result = owner.compareTo(other.owner)
        if (result != 0) {
            return result
        }

        result = name.compareTo(other.name)
        if (result != 0) {
            return result
        }

        return desc.compareTo(other.desc)
    }

    override fun toString(): String {
        return "$owner.$name $desc"
    }

    companion object {
        private val STRING_REGEX = Regex("([^.]+)[.]([^ ]+) (.+)")

        fun fromString(str: String): MemberRef {
            val result = STRING_REGEX.matchEntire(str)
            require(result != null)
            return MemberRef(result.groupValues[1], result.groupValues[2], result.groupValues[3])
        }
    }
}
