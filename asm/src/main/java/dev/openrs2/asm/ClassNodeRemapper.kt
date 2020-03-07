package dev.openrs2.asm

import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.FrameNode
import org.objectweb.asm.tree.InvokeDynamicInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MultiANewArrayInsnNode
import org.objectweb.asm.tree.TypeInsnNode

fun ClassNode.remap(remapper: Remapper) {
    val originalName = name
    name = remapper.mapType(originalName)
    signature = remapper.mapSignature(signature, false)
    superName = remapper.mapType(superName)
    interfaces = interfaces?.map(remapper::mapType)

    for (field in fields) {
        field.name = remapper.mapFieldName(originalName, field.name, field.desc)
        field.desc = remapper.mapDesc(field.desc)
        field.signature = remapper.mapSignature(field.signature, true)
        field.value = remapper.mapValue(field.value)
    }

    for (method in methods) {
        method.name = remapper.mapMethodName(originalName, method.name, method.desc)
        method.desc = remapper.mapMethodDesc(method.desc)
        method.signature = remapper.mapSignature(method.signature, false)
        method.exceptions = method.exceptions.map(remapper::mapType)

        if (method.hasCode()) {
            ClassForNameUtils.remap(remapper, method)

            for (insn in method.instructions) {
                insn.remap(remapper)
            }

            for (tryCatch in method.tryCatchBlocks) {
                tryCatch.type = remapper.mapType(tryCatch.type)
            }
        }
    }
}

private fun AbstractInsnNode.remap(remapper: Remapper) {
    when (this) {
        is FrameNode -> throw UnsupportedOperationException("SKIP_FRAMES and COMPUTE_FRAMES must be used")
        is FieldInsnNode -> {
            val originalOwner = owner
            owner = remapper.mapType(originalOwner)
            name = remapper.mapFieldName(originalOwner, name, desc)
            desc = remapper.mapDesc(desc)
        }
        is MethodInsnNode -> {
            val originalOwner = owner
            owner = remapper.mapType(originalOwner)
            name = remapper.mapMethodName(originalOwner, name, desc)
            desc = remapper.mapDesc(desc)
        }
        is InvokeDynamicInsnNode -> throw UnsupportedOperationException()
        is TypeInsnNode -> desc = remapper.mapType(desc)
        is LdcInsnNode -> cst = remapper.mapValue(cst)
        is MultiANewArrayInsnNode -> desc = remapper.mapType(desc)
    }
}
