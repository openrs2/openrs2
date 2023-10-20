package org.openrs2.asm

import org.objectweb.asm.Type
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodInsnNode

private val ANY_INSN = { _: AbstractInsnNode -> true }

public fun getExpression(
    last: AbstractInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN,
    initialHeight: Int = 0
): List<AbstractInsnNode>? {
    val expr = mutableListOf<AbstractInsnNode>()

    var height = initialHeight
    var insn: AbstractInsnNode? = last
    do {
        val (pops, pushes) = insn!!.stackMetadata
        expr.add(insn)
        if (insn !== last || initialHeight != 0) {
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

public fun getArgumentExpressions(
    invoke: MethodInsnNode,
    filter: (AbstractInsnNode) -> Boolean = ANY_INSN
): List<List<AbstractInsnNode>>? {
    val exprs = mutableListOf<List<AbstractInsnNode>>()

    var insn: AbstractInsnNode = invoke.previous ?: return null

    for (type in Type.getArgumentTypes(invoke.desc)) {
        val expr = getExpression(insn, filter, type.size) ?: return null
        exprs += expr

        insn = expr.first().previous ?: return null
    }

    return exprs.asReversed()
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
