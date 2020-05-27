package dev.openrs2.asm.classpath

import dev.openrs2.asm.MemberRef
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.FieldInsnNode

class FieldInitializer(val instructions: List<AbstractInsnNode>, val version: Int, val maxStack: Int) {
    val dependencies = instructions.asSequence()
        .filterIsInstance<FieldInsnNode>()
        .filter { it.opcode == Opcodes.GETSTATIC }
        .map(::MemberRef)
        .toSet()
}
