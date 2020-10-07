package org.openrs2.asm

import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FieldNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

public data class MemberDesc(val name: String, val desc: String) {
    public constructor(field: FieldNode) : this(field.name, field.desc)
    public constructor(method: MethodNode) : this(method.name, method.desc)
    public constructor(fieldInsn: FieldInsnNode) : this(fieldInsn.name, fieldInsn.desc)
    public constructor(methodInsn: MethodInsnNode) : this(methodInsn.name, methodInsn.desc)
    public constructor(memberRef: MemberRef) : this(memberRef.name, memberRef.desc)

    override fun toString(): String {
        return "$name $desc"
    }
}
