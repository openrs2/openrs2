package dev.openrs2.asm

import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

data class MemberDesc(val name: String, val desc: String) {
    constructor(field: FieldNode) : this(field.name, field.desc)
    constructor(method: MethodNode) : this(method.name, method.desc)
    constructor(fieldInsn: FieldInsnNode) : this(fieldInsn.name, fieldInsn.desc)
    constructor(methodInsn: MethodInsnNode) : this(methodInsn.name, methodInsn.desc)
    constructor(memberRef: MemberRef) : this(memberRef.name, memberRef.desc)

    override fun toString(): String {
        return "$name $desc"
    }
}
