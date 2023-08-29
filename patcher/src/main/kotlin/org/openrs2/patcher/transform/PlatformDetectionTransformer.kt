package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import jakarta.inject.Singleton
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.toAbstractInsnNode
import org.openrs2.asm.transform.Transformer
import org.openrs2.patcher.OperatingSystem

@Singleton
public class PlatformDetectionTransformer : Transformer() {
    private var glBlocks = 0
    private var miscBlocks = 0

    override fun preTransform(classPath: ClassPath) {
        glBlocks = 0
        miscBlocks = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        var match = GL_PLATFORM_DETECTION_MATCHER.match(method).singleOrNull()
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
            var platform = 0

            for (os in OperatingSystem.values()) {
                val nextOs = LabelNode()

                list.add(VarInsnNode(Opcodes.ALOAD, nameVar))
                list.add(LdcInsnNode(os.needle))
                list.add(
                    MethodInsnNode(
                        Opcodes.INVOKEVIRTUAL,
                        STARTS_WITH.owner,
                        STARTS_WITH.name,
                        STARTS_WITH.desc
                    )
                )
                list.add(JumpInsnNode(Opcodes.IFEQ, nextOs))

                for ((i, arch) in os.architectures.withIndex()) {
                    val matchingArch = LabelNode()
                    val nextArch = LabelNode()

                    if (i != os.architectures.size - 1) {
                        for ((j, needle) in arch.needles.withIndex()) {
                            list.add(VarInsnNode(Opcodes.ALOAD, archVar))
                            list.add(LdcInsnNode(needle))
                            list.add(
                                MethodInsnNode(
                                    Opcodes.INVOKEVIRTUAL,
                                    STARTS_WITH.owner,
                                    STARTS_WITH.name,
                                    STARTS_WITH.desc
                                )
                            )

                            if (j != arch.needles.size - 1) {
                                list.add(JumpInsnNode(Opcodes.IFNE, matchingArch))
                            } else {
                                list.add(JumpInsnNode(Opcodes.IFEQ, nextArch))
                            }
                        }
                    }

                    list.add(matchingArch)
                    list.add((platform++).toAbstractInsnNode())
                    list.add(VarInsnNode(Opcodes.ISTORE, platformVar))
                    list.add(JumpInsnNode(Opcodes.GOTO, end))

                    list.add(nextArch)
                }

                list.add(nextOs)
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
        match = MISC_PLATFORM_DETECTION_MATCHER.match(method).singleOrNull()
        if (match != null) {
            // find os.arch and platform ID variables
            val archStore = match[12] as VarInsnNode
            val archVar = archStore.`var`

            val platformStore = match[match.size - 5] as VarInsnNode
            val platformVar = platformStore.`var`

            // generate our own platform detection code
            val list = InsnList()
            val end = LabelNode()
            val os = OperatingSystem.WINDOWS

            for ((i, arch) in os.architectures.withIndex()) {
                val matchingArch = LabelNode()
                val nextArch = LabelNode()

                if (i != os.architectures.size - 1) {
                    for ((j, needle) in arch.needles.withIndex()) {
                        list.add(VarInsnNode(Opcodes.ALOAD, archVar))
                        list.add(LdcInsnNode(needle))
                        list.add(
                            MethodInsnNode(
                                Opcodes.INVOKEVIRTUAL,
                                STARTS_WITH.owner,
                                STARTS_WITH.name,
                                STARTS_WITH.desc
                            )
                        )

                        if (j != arch.needles.size - 1) {
                            list.add(JumpInsnNode(Opcodes.IFNE, matchingArch))
                        } else {
                            list.add(JumpInsnNode(Opcodes.IFEQ, nextArch))
                        }
                    }
                }

                list.add(matchingArch)
                list.add(i.toAbstractInsnNode())
                list.add(VarInsnNode(Opcodes.ISTORE, platformVar))
                list.add(JumpInsnNode(Opcodes.GOTO, end))

                list.add(nextArch)
            }

            list.add(end)

            // replace existing platform detection code with our own
            for (i in (13 until match.size - 4)) {
                method.instructions.remove(match[i])
            }

            method.instructions.insert(match[12], list)

            miscBlocks++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Replaced $glBlocks jaggl and $miscBlocks jagmisc platform detection blocks" }
    }

    private companion object {
        private val logger = InlineLogger()
        private val GL_PLATFORM_DETECTION_MATCHER = InsnMatcher.compile(
            """
            LDC INVOKESTATIC INVOKEVIRTUAL ASTORE
            LDC INVOKESTATIC INVOKEVIRTUAL ASTORE
            .*
            ICONST ISTORE ILOAD GETSTATIC ILOAD AALOAD ARRAYLENGTH
            """
        )
        private val MISC_PLATFORM_DETECTION_MATCHER = InsnMatcher.compile(
            """
            LDC INVOKESTATIC INVOKEVIRTUAL ASTORE ALOAD LDC INVOKEVIRTUAL IFNE GOTO
            LDC INVOKESTATIC INVOKEVIRTUAL ASTORE
            .*
            ICONST ISTORE ALOAD ALOAD LDC INVOKEVIRTUAL
        """
        )
        private val STARTS_WITH = MemberRef("java/lang/String", "startsWith", "(Ljava/lang/String;)Z")

        private fun findUnknownOs(match: List<AbstractInsnNode>): InsnList {
            val aloadIndex = match.indexOfFirst {
                it is VarInsnNode && it.opcode == Opcodes.ALOAD && it.`var` == 0
            }
            require(aloadIndex != -1) { "Missing ALOAD_0" }

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
