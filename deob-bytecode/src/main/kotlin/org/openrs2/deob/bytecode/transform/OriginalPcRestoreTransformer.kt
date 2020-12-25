package org.openrs2.deob.bytecode.transform

import com.github.michaelbull.logging.InlineLogger
import org.objectweb.asm.Type
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.LabelNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.classpath.Library
import org.openrs2.asm.removeDeadCode
import org.openrs2.asm.transform.Transformer
import org.openrs2.deob.annotation.OriginalMember
import org.openrs2.deob.bytecode.OriginalPcTable
import org.openrs2.deob.util.map.NameMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class OriginalPcRestoreTransformer @Inject constructor(private val nameMap: NameMap) : Transformer() {
    private var originalPcsRestored = 0

    override fun preTransform(classPath: ClassPath) {
        originalPcsRestored = 0
    }

    override fun transformCode(classPath: ClassPath, library: Library, clazz: ClassNode, method: MethodNode): Boolean {
        method.removeDeadCode(clazz.name)

        val pcs = mutableMapOf<LabelNode, Int>()

        for (insn in method.instructions) {
            if (insn.opcode == -1) {
                continue
            }

            val originalPc = classPath.originalPcs[insn] ?: continue

            val label = LabelNode()
            method.instructions.insertBefore(insn, label)

            pcs[label] = originalPc

            originalPcsRestored++
        }

        val originalMember = method.getOriginalMember()
        val names: Map<Int, String> = if (originalMember != null) {
            nameMap.methods[originalMember]?.locals ?: emptyMap()
        } else {
            emptyMap()
        }

        if (method.attrs == null) {
            method.attrs = mutableListOf()
        }
        method.attrs.add(OriginalPcTable(pcs, names))

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Restored $originalPcsRestored original instruction indexes" }
    }

    private fun MethodNode.getOriginalMember(): MemberRef? {
        if (invisibleAnnotations == null) {
            return null
        }

        for (annotation in invisibleAnnotations) {
            if (annotation.desc != Type.getDescriptor(OriginalMember::class.java)) {
                continue
            }

            var owner: String? = null
            var name: String? = null
            var desc: String? = null
            for ((key, value) in annotation.values.windowed(2, 2)) {
                when (key) {
                    "owner" -> owner = value as String
                    "name" -> name = value as String
                    "descriptor" -> desc = value as String
                }
            }

            check(owner != null) { "Failed to extract owner from OriginalMember" }
            check(name != null) { "Failed to extract name from OriginalMember" }
            check(desc != null) { "Failed to extract descriptor from OriginalMember" }

            return MemberRef(owner, name, desc)
        }

        return null
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
