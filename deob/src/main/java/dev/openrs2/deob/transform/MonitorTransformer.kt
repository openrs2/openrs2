package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.nextReal
import dev.openrs2.asm.previousReal
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode

class MonitorTransformer : Transformer() {
    private var subroutinesInlined = 0
    private var tryRangesExtended = 0
    private var loadsReplaced = 0

    override fun preTransform(classPath: ClassPath) {
        subroutinesInlined = 0
        tryRangesExtended = 0
        loadsReplaced = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        inlineSubroutines(method)
        extendTryRanges(method)
        replaceLoadWithDup(method)
        return false
    }

    private fun inlineSubroutines(method: MethodNode) {
        val subroutines = mutableMapOf<AbstractInsnNode, List<AbstractInsnNode>>()
        for (match in SUBROUTINE_MATCHER.match(method.instructions)) {
            subroutines[match[0]] = match
        }

        for (match in JSR_MATCHER.match(method.instructions)) {
            val jsr = match[1] as JumpInsnNode
            val subroutine = subroutines[jsr.label.nextReal] ?: continue

            val ret = subroutine[3] as JumpInsnNode
            if (ret.label.nextReal != jsr.nextReal) {
                continue
            }

            val clonedLabels = emptyMap<LabelNode, LabelNode>()
            method.instructions.set(match[0], subroutine[1].clone(clonedLabels))
            method.instructions.set(match[1], subroutine[2].clone(clonedLabels))

            subroutinesInlined++
        }
    }

    private fun extendTryRanges(method: MethodNode) {
        for (tryCatch in method.tryCatchBlocks) {
            if (tryCatch.type != null) {
                continue
            }

            val monitorenter = tryCatch.start.previousReal ?: continue
            if (monitorenter.opcode != Opcodes.MONITORENTER) {
                continue
            }

            // extend the try to cover the ALOAD and MONTITOREXIT instructions
            val aload = tryCatch.end.nextReal ?: continue
            if (aload.opcode != Opcodes.ALOAD) {
                continue
            }

            val monitorexit = aload.nextReal ?: continue
            if (monitorexit.opcode != Opcodes.MONITOREXIT) {
                continue
            }

            val end = monitorexit.nextReal ?: continue

            val label = LabelNode()
            method.instructions.insertBefore(end, label)
            tryCatch.end = label

            tryRangesExtended++
        }
    }

    private fun replaceLoadWithDup(method: MethodNode) {
        for (match in LOAD_MATCHER.match(method.instructions)) {
            method.instructions.insertBefore(match[0], InsnNode(Opcodes.DUP))
            method.instructions.remove(match[1])

            loadsReplaced++
        }
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Inlined $subroutinesInlined MONITOREXIT subroutines" }
        logger.info { "Extended $tryRangesExtended try ranges to cover MONITOREXIT instructions" }
        logger.info { "Replaced $loadsReplaced ASTORE ALOAD sequences with DUP ASTORE" }
    }

    companion object {
        private val logger = InlineLogger()

        // these regexes rely on JSRInlinerAdapter running first
        private val JSR_MATCHER = InsnMatcher.compile("ACONST_NULL GOTO")
        private val SUBROUTINE_MATCHER = InsnMatcher.compile("ASTORE ALOAD MONITOREXIT GOTO")

        private val LOAD_MATCHER = InsnMatcher.compile("ASTORE ALOAD MONITORENTER")
    }
}
