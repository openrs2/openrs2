package dev.openrs2.asm

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

private val ANY_INSN = { _: AbstractInsnNode -> true }

fun getExpression(
    last: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): List<AbstractInsnNode>? {
    val expr = mutableListOf<AbstractInsnNode>()

    var height = 0
    var insn: AbstractInsnNode? = last
    do {
        val (pops, pushes) = insn!!.stackMetadata
        if (insn !== last) {
            expr.add(insn)
            height -= pushes
        }
        height += pops

        if (height == 0) {
            return expr.asReversed()
        }

        insn = insn.previous
    } while (insn != null && insn.sequential && filter(insn))

    return null
}

fun InsnList.replaceExpression(
    last: AbstractInsnNode,
    replacement: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): Boolean {
    val expr = getExpression(last, filter) ?: return false
    expr.forEach(this::remove)
    this[last] = replacement
    return true
}

fun InsnList.deleteExpression(
    last: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): Boolean {
    val expr = getExpression(last, filter) ?: return false
    expr.forEach(this::remove)
    remove(last)
    return true
}
