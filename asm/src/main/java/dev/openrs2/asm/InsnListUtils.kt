package dev.openrs2.asm

import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.InsnList

fun InsnList.replaceSimpleExpression(last: AbstractInsnNode, replacement: AbstractInsnNode?): Boolean {
    val deadInsns = mutableListOf<AbstractInsnNode>()

    var height = 0
    var insn: AbstractInsnNode? = last
    do {
        val (pops, pushes) = insn!!.stackMetadata()
        if (insn !== last) {
            deadInsns.add(insn)
            height -= pushes
        }
        height += pops

        if (height == 0) {
            deadInsns.forEach(this::remove)

            if (replacement != null) {
                this[last] = replacement
            } else {
                remove(last)
            }

            return true
        }

        insn = insn.previous
    } while (insn != null && insn.type != AbstractInsnNode.LABEL && insn.pure)

    return false
}

fun InsnList.deleteSimpleExpression(last: AbstractInsnNode): Boolean {
    return replaceSimpleExpression(last, null)
}
