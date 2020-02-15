package dev.openrs2.asm

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

fun getSimpleExpression(last: AbstractInsnNode): List<AbstractInsnNode>? {
    val expr = mutableListOf<AbstractInsnNode>()

    var height = 0
    var insn: AbstractInsnNode? = last
    do {
        val (pops, pushes) = insn!!.stackMetadata()
        if (insn !== last) {
            expr.add(insn)
            height -= pushes
        }
        height += pops

        if (height == 0) {
            return expr.asReversed()
        }

        insn = insn.previous
    } while (insn != null && insn.type != AbstractInsnNode.LABEL && insn.pure)

    return null
}

fun InsnList.replaceSimpleExpression(last: AbstractInsnNode, replacement: AbstractInsnNode): Boolean {
    val expr = getSimpleExpression(last) ?: return false
    expr.forEach(this::remove)
    this[last] = replacement
    return true
}

fun InsnList.deleteSimpleExpression(last: AbstractInsnNode): Boolean {
    val expr = getSimpleExpression(last) ?: return false
    expr.forEach(this::remove)
    remove(last)
    return true
}
