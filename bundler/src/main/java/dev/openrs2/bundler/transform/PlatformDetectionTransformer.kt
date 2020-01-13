package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.createIntConstant
import dev.openrs2.asm.intConstant
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode

class PlatformDetectionTransformer : Transformer() {
    private var glBlocks = 0
    private var miscBlocks = 0

    override fun preTransform(classPath: ClassPath) {
        glBlocks = 0
        miscBlocks = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        val match = GL_PLATFORM_DETECTION_MATCHER.match(method).singleOrNull()
        if (match != null) {
            // find os.name, os.arch and platform ID variables
            val nameStore = match[3] as VarInsnNode
            val nameVar = nameStore.`var`

            val archStore = match[7] as VarInsnNode
            val archVar = archStore.`var`

            val platformLoad = match[match.size - 3] as VarInsnNode
            val platformVar = platformLoad.`var`

            // find unknown OS call
            val unknownOs = findUnknownOs(match)

            // generate our own platform detection code
            val list = InsnList()
            val end = LabelNode()

            for ((index, os) in OS_NAMES.withIndex()) {
                val next = LabelNode()
                val amd64 = LabelNode()
                val i386 = LabelNode()

                list.add(VarInsnNode(Opcodes.ALOAD, nameVar))
                list.add(LdcInsnNode(os))
                list.add(
                    MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        STARTS_WITH.owner,
                        STARTS_WITH.name,
                        STARTS_WITH.desc
                    )
                )
                list.add(JumpInsnNode(Opcodes.IFEQ, next))

                list.add(VarInsnNode(Opcodes.ALOAD, archVar))
                list.add(LdcInsnNode("amd64"))
                list.add(
                    MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        STARTS_WITH.owner,
                        STARTS_WITH.name,
                        STARTS_WITH.desc
                    )
                )
                list.add(JumpInsnNode(Opcodes.IFNE, amd64))

                list.add(VarInsnNode(Opcodes.ALOAD, archVar))
                list.add(LdcInsnNode("x86_64"))
                list.add(
                    MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        STARTS_WITH.owner,
                        STARTS_WITH.name,
                        STARTS_WITH.desc
                    )
                )
                list.add(JumpInsnNode(Opcodes.IFEQ, i386))

                list.add(amd64)
                list.add(createIntConstant(index * 2 + 1))
                list.add(VarInsnNode(Opcodes.ISTORE, platformVar))
                list.add(JumpInsnNode(Opcodes.GOTO, end))

                list.add(i386)
                list.add(createIntConstant(index * 2))
                list.add(VarInsnNode(Opcodes.ISTORE, platformVar))
                list.add(JumpInsnNode(Opcodes.GOTO, end))

                list.add(next)
            }

            list.add(unknownOs)
            list.add(end)

            // replace existing platform detection code with our own
            for (i in (8 until match.size - 7)) {
                method.instructions.remove(match[i])
            }

            method.instructions.insert(match[7], list)

            glBlocks++
        }

        // adjust jagmisc platform IDs to account for the removal of MSJVM support
        val miscMatch = MISC_PLATFORM_DETECTION_MATCHER.match(method).filter {
            val const1 = it[0].intConstant
            if (const1 != 0 && const1 != 2) {
                return@filter false
            }

            val const2 = it[it.size - 2].intConstant
            if (const2 != 0 && const2 != 2) {
                return@filter false
            }

            if (const1 == const2) {
                return@filter false
            }

            val store1 = it[1] as VarInsnNode
            val store2 = it[it.size - 1] as VarInsnNode
            return@filter store1.`var` == store2.`var`
        }.singleOrNull()
        if (miscMatch != null) {
            val iconst = miscMatch.single { it.intConstant == 2 }
            method.instructions.set(iconst, InsnNode(Opcodes.ICONST_1))
            miscBlocks++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $glBlocks jaggl and $miscBlocks jagmisc platform detection blocks" }
    }

    companion object {
        private val logger = InlineLogger()
        private val GL_PLATFORM_DETECTION_MATCHER = InsnMatcher.compile(
            """
            LDC INVOKESTATIC INVOKEVIRTUAL ASTORE
            LDC INVOKESTATIC INVOKEVIRTUAL ASTORE
            .*
            ICONST ISTORE ILOAD GETSTATIC ILOAD AALOAD ARRAYLENGTH
            """
        )
        private val MISC_PLATFORM_DETECTION_MATCHER =
            InsnMatcher.compile("ICONST ISTORE ((GETSTATIC | ILOAD) IFEQ | GOTO) ICONST ISTORE")
        private val OS_NAMES = listOf("win", "mac", "linux")
        private val STARTS_WITH = MemberRef("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")

        private fun findUnknownOs(match: List<AbstractInsnNode>): InsnList {
            val aloadIndex = match.indexOfFirst {
                it is VarInsnNode && it.opcode == Opcodes.ALOAD && it.`var` == 0
            }
            if (aloadIndex == -1) {
                throw IllegalArgumentException("Missing ALOAD_0")
            }

            val list = InsnList()
            for (i in aloadIndex until match.size) {
                val insn = match[i]
                list.add(insn.clone(emptyMap()))

                if (insn.opcode == Opcodes.RETURN) {
                    return list
                }
            }

            throw IllegalArgumentException("Missing RETURN")
        }
    }
}
