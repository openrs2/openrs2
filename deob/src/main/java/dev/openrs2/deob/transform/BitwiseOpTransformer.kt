package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.hasCode
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import javax.inject.Singleton

@Singleton
class BitwiseOpTransformer : Transformer() {
    private val methodOps = mutableMapOf<MemberRef, Int>()
    private var inlinedOps = 0

    override fun preTransform(classPath: ClassPath) {
        methodOps.clear()
        inlinedOps = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (!method.hasCode()) {
                        continue
                    }

                    if (method.access and Opcodes.ACC_STATIC == 0) {
                        continue
                    }

                    if (method.desc != BITWISE_OP_DESC) {
                        continue
                    }

                    val match = BITWISE_OP_MATCHER.match(method).firstOrNull() ?: continue

                    val iload0 = match[0] as VarInsnNode
                    val iload1 = match[1] as VarInsnNode
                    if (iload0.`var` != 0 || iload1.`var` != 1) {
                        continue
                    }

                    val methodRef = MemberRef(clazz, method)
                    methodOps[methodRef] = match[2].opcode
                }
            }
        }
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        clazz.methods.removeIf { methodOps.containsKey(MemberRef(clazz, it)) }
        return false
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val it = method.instructions.iterator()
        while (it.hasNext()) {
            val insn = it.next()
            if (insn !is MethodInsnNode || insn.opcode != Opcodes.INVOKESTATIC) {
                continue
            }

            val opcode = methodOps[MemberRef(insn)]
            if (opcode != null) {
                it.set(InsnNode(opcode))
                inlinedOps++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Inlined $inlinedOps bitwise ops and removed ${methodOps.size} redundant methods" }
    }

    companion object {
        private val logger = InlineLogger()
        private val BITWISE_OP_MATCHER = InsnMatcher.compile("^ILOAD ILOAD (IXOR | IAND | IOR) IRETURN$")
        private const val BITWISE_OP_DESC = "(II)I"
    }
}
