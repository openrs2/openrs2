package dev.openrs2.asm

import org.objectweb.asm.commons.Remapper
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode

public object ClassForNameUtils {
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

    private fun findLdcInsns(method: MethodNode): Sequence<LdcInsnNode> {
        return INVOKE_MATCHER.match(method)
            .filter(::isClassForName)
            .map { it[0] as LdcInsnNode }
    }

    private fun internalName(ldc: LdcInsnNode): String {
        return (ldc.cst as String).toInternalClassName()
    }

    public fun findClassNames(method: MethodNode): Sequence<String> {
        return findLdcInsns(method).map(::internalName)
    }

    public fun remap(remapper: Remapper, method: MethodNode) {
        for (ldc in findLdcInsns(method)) {
            val name = remapper.mapType(internalName(ldc))
            if (name != null) {
                ldc.cst = name.toBinaryClassName()
            }
        }
    }
}
