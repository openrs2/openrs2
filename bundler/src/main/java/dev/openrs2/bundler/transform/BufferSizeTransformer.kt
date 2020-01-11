package dev.openrs2.bundler.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.createIntConstant
import dev.openrs2.asm.hasCode
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.LdcInsnNode
import org.objectweb.asm.tree.MethodNode
import kotlin.streams.asSequence

class BufferSizeTransformer : Transformer() {
    private var buffer: MemberRef? = null
    private var buffersResized = 0

    override fun preTransform(classPath: ClassPath) {
        buffer = null
        buffersResized = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (!method.hasCode()) {
                        continue
                    }

                    this.buffer = findBuffer(method) ?: continue
                    logger.info { "Identified buffer: ${this.buffer}" }
                    break
                }
            }
        }
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        if (buffer == null) {
            return false
        }

        NEW_BUFFER_MATCHER.match(method).forEach {
            val putstatic = it[4] as FieldInsnNode
            if (MemberRef(putstatic) == buffer!!) {
                method.instructions[it[2]] = createIntConstant(65536)
                buffersResized++
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Resized $buffersResized buffers to 65536 bytes" }
    }

    companion object {
        private val logger = InlineLogger()
        private val GPP1_POS_MATCHER = InsnMatcher.compile("LDC (INVOKESPECIAL | INVOKEVIRTUAL) GETSTATIC")
        private val NEW_BUFFER_MATCHER = InsnMatcher.compile("NEW DUP (SIPUSH | LDC) INVOKESPECIAL PUTSTATIC")

        private fun findBuffer(method: MethodNode): MemberRef? {
            return GPP1_POS_MATCHER.match(method).asSequence().filter {
                val ldc = it[0] as LdcInsnNode
                ldc.cst == "gpp1 pos:"
            }.map {
                val getstatic = it[2] as FieldInsnNode
                MemberRef(getstatic)
            }.firstOrNull()
        }
    }
}
