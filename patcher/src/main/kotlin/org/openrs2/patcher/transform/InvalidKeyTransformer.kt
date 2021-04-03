package org.openrs2.patcher.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.InsnList
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.IntInsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.transform.Transformer
import javax.inject.Singleton

@Singleton
public class InvalidKeyTransformer : Transformer() {
    private var catchBlocks = 0

    override fun preTransform(classPath: ClassPath) {
        catchBlocks = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        for (match in MATCHER.match(method)) {
            val ldc = if (match[8] is LdcInsnNode) {
                match[8]
            } else {
                match[9]
            } as LdcInsnNode

            if (ldc.cst != "T3 - ") {
                continue
            }

            val uncompressedStore = match[2] as VarInsnNode
            val goto = match[3] as JumpInsnNode
            val exLoad = match[5]
            val invokeWrap = match[match.size - 2]
            val exThrow = match[match.size - 1]

            /*
             * Replace:
             *
             *     } catch (RuntimeException ex) {
             *         throw TracingException.wrap(ex, "T3 - " + ...);
             *     }
             *
             * with:
             *
             *     } catch (RuntimeException ex) {
             *         System.out.println("T3 - " + ...);
             *         uncompressed = new byte[] { 0, 0, 0, 0, 1, 1 };
             *     }
             */
            val list = InsnList()

            list.add(FieldInsnNode(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;"))
            list.add(InsnNode(Opcodes.SWAP))
            list.add(MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V"))

            list.add(IntInsnNode(Opcodes.BIPUSH, 6))
            list.add(IntInsnNode(Opcodes.NEWARRAY, Opcodes.T_BYTE))

            list.add(InsnNode(Opcodes.DUP))
            list.add(InsnNode(Opcodes.ICONST_0))
            list.add(InsnNode(Opcodes.ICONST_0))
            list.add(InsnNode(Opcodes.BASTORE))

            list.add(VarInsnNode(Opcodes.ASTORE, uncompressedStore.`var`))
            list.add(JumpInsnNode(Opcodes.GOTO, goto.label))

            method.instructions.remove(exLoad)
            method.instructions.insertBefore(invokeWrap, list)
            method.instructions.remove(invokeWrap)
            method.instructions.remove(exThrow)

            catchBlocks++
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Made $catchBlocks invalid key catch blocks non-fatal" }
    }

    private companion object {
        private val logger = InlineLogger()

        private val MATCHER = InsnMatcher.compile(
            """
            ALOAD INVOKESTATIC ASTORE GOTO ASTORE ALOAD NEW DUP INVOKESPECIAL? LDC .*? INVOKESTATIC ATHROW
        """.trimIndent()
        )
    }
}
