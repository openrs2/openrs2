package org.openrs2.asm

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode

private val ANY_INSN = { _: AbstractInsnNode -> true }

public fun getExpression(
    last: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): List<AbstractInsnNode>? {
    val expr = mutableListOf<AbstractInsnNode>()

    var height = 0
    var insn: AbstractInsnNode? = last
    do {
        val (pops, pushes) = insn!!.stackMetadata
        expr.add(insn)
        if (insn !== last) {
            height -= pushes
        }
        height += pops

        if (height == 0) {
            return expr.asReversed()
        }

        insn = insn.previous
    } while (insn != null && insn.isSequential && filter(insn))

    return null
}

public fun InsnList.replaceExpression(
    last: AbstractInsnNode,
    replacement: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): Boolean {
    val expr = getExpression(last, filter) ?: return false
    expr.filter { it !== last }.forEach(this::remove)
    this[last] = replacement
    return true
}

public fun InsnList.deleteExpression(
    last: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): Boolean {
    val expr = getExpression(last, filter) ?: return false
    expr.forEach(this::remove)
    return true
}

public fun InsnList.clone(labels: Map<LabelNode, LabelNode>): InsnList {
    val copy = InsnList()
    for (insn in this) {
        copy.add(insn.clone(labels))
    }
    return copy
}
