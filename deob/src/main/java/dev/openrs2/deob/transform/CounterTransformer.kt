package dev.openrs2.deob.transform

import com.github.michaelbull.logging.InlineLogger
import dev.openrs2.asm.InsnMatcher
import dev.openrs2.asm.MemberRef
import dev.openrs2.asm.classpath.ClassPath
import dev.openrs2.asm.classpath.Library
import dev.openrs2.asm.hasCode
import dev.openrs2.asm.transform.Transformer
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode

class CounterTransformer : Transformer() {
    private val counters = mutableSetOf<MemberRef>()

    override fun preTransform(classPath: ClassPath) {
        counters.clear()

        val references = mutableMapOf<MemberRef, Int>()
        val resets = mutableMapOf<MemberRef, Int>()
        val increments = mutableMapOf<MemberRef, Int>()

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (method.hasCode()) {
                        findCounters(method, references, resets, increments)
                    }
                }
            }
        }

        deleteCounters(classPath, references, resets, increments)
    }

    private fun findCounters(
        method: MethodNode,
        references: MutableMap<MemberRef, Int>,
        resets: MutableMap<MemberRef, Int>,
        increments: MutableMap<MemberRef, Int>
    ) {
        for (insn in method.instructions) {
            if (insn is FieldInsnNode) {
                references.merge(MemberRef(insn), 1, Integer::sum)
            }
        }

        RESET_PATTERN.match(method).forEach {
            val putstatic = MemberRef(it[1] as FieldInsnNode)
            resets.merge(putstatic, 1, Integer::sum)
        }

        INCREMENT_PATTERN.match(method).forEach {
            val getstatic = MemberRef(it[0] as FieldInsnNode)
            val putstatic = MemberRef(it[3] as FieldInsnNode)
            if (getstatic == putstatic) {
                increments.merge(putstatic, 1, Integer::sum)
            }
        }
    }

    private fun deleteCounters(
        classPath: ClassPath,
        references: Map<MemberRef, Int>,
        resets: Map<MemberRef, Int>,
        increments: Map<MemberRef, Int>
    ) {
        for ((counter, value) in references) {
            // one for the reset, two for the increment
            if (value != 3) {
                continue
            }

            if (resets[counter] != 1) {
                continue
            }

            if (increments[counter] != 1) {
                continue
            }

            val owner = classPath.getNode(counter.owner)!!
            owner.fields.removeIf { it.name == counter.name && it.desc == counter.desc }
            counters.add(counter)
        }
    }

    override fun transformCode(
        classPath: ClassPath,
        library: Library,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        RESET_PATTERN.match(method).forEach {
            val putstatic = it[1] as FieldInsnNode
            if (counters.contains(MemberRef(putstatic))) {
                it.forEach(method.instructions::remove)
            }
        }

        INCREMENT_PATTERN.match(method).forEach {
            val getstatic = MemberRef(it[0] as FieldInsnNode)
            val putstatic = MemberRef(it[3] as FieldInsnNode)
            if (getstatic == putstatic && counters.contains(putstatic)) {
                it.forEach(method.instructions::remove)
            }
        }

        return false
    }

    override fun postTransform(classPath: ClassPath) {
        logger.info { "Removed ${counters.size} counters" }
    }

    companion object {
        private val logger = InlineLogger()
        private val RESET_PATTERN = InsnMatcher.compile("ICONST_0 PUTSTATIC")
        private val INCREMENT_PATTERN = InsnMatcher.compile("GETSTATIC ICONST_1 IADD PUTSTATIC")
    }
}
