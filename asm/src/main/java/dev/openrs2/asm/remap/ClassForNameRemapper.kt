package dev.openrs2.asm.remap

import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.toBinaryClassName
import dev.openrs2.asm.toInternalClassName
import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

object ClassForNameRemapper {
    private val INVOKE_MATCHER = InsnMatcher.compile("LDC INVOKESTATIC")

    private fun isClassForName(match: List<AbstractInsnNode>): Boolean {
        val ldc = match[0] as LdcInsnNode
        if (ldc.cst !is String) {
            return false
        }

        val invokestatic = match[1] as MethodInsnNode
        return invokestatic.owner == "java/lang/Class" &&
            invokestatic.name == "forName" &&
            invokestatic.desc == "(Ljava/lang/String;)Ljava/lang/Class;"
    }

    fun remap(remapper: Remapper, method: MethodNode) {
        for (match in INVOKE_MATCHER.match(method).filter(ClassForNameRemapper::isClassForName)) {
            val ldc = match[0] as LdcInsnNode
            val name = remapper.map((ldc.cst as String).toInternalClassName())
            if (name != null) {
                ldc.cst = name.toBinaryClassName()
            }
        }
    }
}
