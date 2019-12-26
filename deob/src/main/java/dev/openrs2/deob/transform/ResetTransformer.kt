package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.hasCode
import dev.openrs2.asm.nextReal
import dev.openrs2.asm.removeDeadCode
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*

class ResetTransformer : Transformer() {
    private val resetMethods = mutableSetOf<MemberRef>()

    override fun preTransform(classPath: ClassPath) {
        resetMethods.clear()

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (!method.hasCode()) {
                        continue
                    }

                    val masterReset = findMasterReset(method) ?: continue
                    logger.info { "Identified master reset method: $masterReset" }

                    val resetClass = classPath.getNode("client")!!
                    val resetMethod = resetClass.methods.first {
                        it.name == masterReset.name && it.desc == masterReset.desc
                    }

                    findResetMethods(resetMethods, resetClass, resetMethod)

                    resetMethod.instructions.clear()
                    resetMethod.tryCatchBlocks.clear()
                    resetMethod.instructions.add(InsnNode(Opcodes.RETURN))
                }
            }
        }
    }

    override fun transformClass(classPath: ClassPath, library: Library, clazz: ClassNode): Boolean {
        clazz.methods.removeIf { resetMethods.contains(MemberRef(clazz, it)) }
        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed ${resetMethods.size} reset methods" }
    }

    companion object {
        private val logger = InlineLogger()

        private fun findMasterReset(method: MethodNode): MemberRef? {
            var shutdownLdc: AbstractInsnNode? = null
            for (insn in method.instructions) {
                if (insn is LdcInsnNode && insn.cst == "Shutdown complete - clean:") {
                    shutdownLdc = insn
                    break
                }
            }

            var insn = shutdownLdc
            while (insn != null) {
                if (insn !is VarInsnNode || insn.opcode != Opcodes.ALOAD) {
                    insn = insn.previous
                    continue
                }

                if (insn.`var` != 0) {
                    insn = insn.previous
                    continue
                }

                val nextInsn = insn.nextReal
                if (nextInsn !is MethodInsnNode || nextInsn.opcode != Opcodes.INVOKEVIRTUAL) {
                    insn = insn.previous
                    continue
                }

                if (nextInsn.desc != "()V") {
                    insn = insn.previous
                    continue
                }

                return MemberRef(nextInsn)
            }

            return null
        }

        private fun findResetMethods(resetMethods: MutableSet<MemberRef>, clazz: ClassNode, method: MethodNode) {
            method.removeDeadCode(clazz.name)

            for (insn in method.instructions) {
                if (insn is MethodInsnNode && insn.opcode == Opcodes.INVOKESTATIC) {
                    resetMethods.add(MemberRef(insn))
                }
            }
        }
    }
}
