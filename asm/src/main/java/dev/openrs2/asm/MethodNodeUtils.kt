package dev.openrs2.asm

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.IincInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.objectweb.asm.tree.analysis.Analyzer
import org.objectweb.asm.tree.analysis.BasicInterpreter
import org.objectweb.asm.tree.analysis.BasicValue

private fun localIndex(access: Int, argTypes: Array<Type>, argIndex: Int): Int {
    var localIndex = 0
    if (access and Opcodes.ACC_STATIC == 0) {
        localIndex++
    }
    for (i in 0 until argIndex) {
        localIndex += argTypes[i].size
    }
    return localIndex
}

private fun remap(i: Int, argType: Type, localIndex: Int): Int {
    return if (i >= localIndex) {
        i - argType.size
    } else {
        i
    }
}

private fun remapAll(indexes: List<Int>, argType: Type, localIndex: Int): MutableList<Int> {
    return indexes.map { remap(it, argType, localIndex) }.toMutableList()
}

fun MethodNode.removeArgument(argIndex: Int) {
    // remove argument from the descriptor
    val type = Type.getType(desc)
    val argType = type.argumentTypes[argIndex]
    val argTypes = type.argumentTypes.filterIndexed { index, _ -> index != argIndex }.toTypedArray()
    desc = Type.getMethodDescriptor(type.returnType, *argTypes)

    // the client doesn't use signatures so don't bother with them
    if (signature != null) {
        throw UnsupportedOperationException("Signatures unsupported")
    }

    parameters?.removeAt(argIndex)

    // remove annotations
    if (visibleAnnotableParameterCount != 0) {
        throw UnsupportedOperationException("Non-zero visibleAnnotableParameterCount unsupported")
    }

    if (visibleParameterAnnotations != null) {
        visibleParameterAnnotations =
            visibleParameterAnnotations.filterIndexed { index, _ -> index != argIndex }.toTypedArray()
    }

    if (invisibleAnnotableParameterCount != 0) {
        throw UnsupportedOperationException("Non-zero invisibleAnnotableParameterCount unsupported")
    }

    if (invisibleParameterAnnotations != null) {
        invisibleParameterAnnotations =
            invisibleParameterAnnotations.filterIndexed { index, _ -> index != argIndex }.toTypedArray()
    }

    // remap locals
    val localIndex = localIndex(access, argTypes, argIndex)
    maxLocals -= argType.size

    if (localVariables != null) {
        localVariables.removeIf { it.index == localIndex }
        localVariables.forEach { it.index = remap(it.index, argType, localIndex) }
    }

    if (visibleLocalVariableAnnotations != null) {
        visibleLocalVariableAnnotations.removeIf { it.index.contains(localIndex) }
        visibleLocalVariableAnnotations.forEach { it.index = remapAll(it.index, argType, localIndex) }
    }

    if (invisibleLocalVariableAnnotations != null) {
        invisibleLocalVariableAnnotations.removeIf { it.index.contains(localIndex) }
        invisibleLocalVariableAnnotations.forEach { it.index = remapAll(it.index, argType, localIndex) }
    }

    for (insn in instructions) {
        when (insn) {
            is VarInsnNode -> insn.`var` = remap(insn.`var`, argType, localIndex)
            is IincInsnNode -> insn.`var` = remap(insn.`var`, argType, localIndex)
            is FrameNode -> {
                if (insn.type != Opcodes.F_NEW) {
                    throw UnsupportedOperationException("Only F_NEW frames are supported")
                }

                for (i in 0 until argType.size) {
                    insn.local.removeAt(localIndex)
                }
            }
        }
    }
}

fun MethodNode.removeDeadCode(owner: String) {
    var changed: Boolean
    do {
        changed = false

        val analyzer = Analyzer<BasicValue>(BasicInterpreter())
        val frames = analyzer.analyze(owner, this)

        val deadLabels = mutableSetOf<LabelNode>()
        val it = instructions.iterator()
        var i = 0
        for (insn in it) {
            if (frames[i++] != null) {
                continue
            }

            if (insn is LabelNode) {
                deadLabels.add(insn)
            } else {
                it.remove()
                changed = true
            }
        }

        changed = changed or tryCatchBlocks.removeIf { deadLabels.contains(it.start) && deadLabels.contains(it.end) }
    } while (changed)
}

fun MethodNode.hasCode(): Boolean {
    return access and (Opcodes.ACC_NATIVE or Opcodes.ACC_ABSTRACT) == 0
}
