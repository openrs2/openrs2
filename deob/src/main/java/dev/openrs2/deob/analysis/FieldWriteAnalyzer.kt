package dev.openrs2.deob.analysis

import dev.openrs2.asm.MemberDesc
import dev.openrs2.asm.classpath.ClassPath
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.FieldInsnNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.analysis.Frame

class FieldWriteAnalyzer(
    private val clazz: ClassNode,
    private val method: MethodNode,
    private val classPath: ClassPath,
    private val frames: Array<Frame<ThisValue>>
) : DataFlowAnalyzer<Map<MemberDesc, FieldWriteCount>>(clazz.name, method) {
    override fun createEntrySet(): Map<MemberDesc, FieldWriteCount> {
        val set = mutableMapOf<MemberDesc, FieldWriteCount>()

        for (field in clazz.fields) {
            set[MemberDesc(field)] = FieldWriteCount.NEVER
        }

        return set
    }

    override fun createInitialSet(): Map<MemberDesc, FieldWriteCount> {
        return emptyMap()
    }

    override fun join(
        set1: Map<MemberDesc, FieldWriteCount>,
        set2: Map<MemberDesc, FieldWriteCount>
    ): Map<MemberDesc, FieldWriteCount> {
        if (set1 == set2) {
            return set1
        }

        val set = mutableMapOf<MemberDesc, FieldWriteCount>()

        for (member in set1.keys union set2.keys) {
            val count1 = set1[member]
            val count2 = set2[member]

            set[member] = when {
                count1 == null && count2 != null -> count2
                count2 == null && count1 != null -> count1
                count1 == count2 -> count1!!
                else -> FieldWriteCount.ONCE_OR_MORE
            }
        }

        return set
    }

    override fun transfer(
        set: Map<MemberDesc, FieldWriteCount>,
        insn: AbstractInsnNode
    ): Map<MemberDesc, FieldWriteCount> {
        if (insn !is FieldInsnNode) {
            return set
        } else if (insn.opcode != Opcodes.PUTFIELD && insn.opcode != Opcodes.PUTSTATIC) {
            return set
        }

        val member = MemberDesc(insn)
        val declaredOwner = classPath[insn.owner]!!.resolveField(member)!!.name
        if (declaredOwner != clazz.name) {
            return set
        }

        val isThis = if (insn.opcode == Opcodes.PUTFIELD) {
            val insnIndex = method.instructions.indexOf(insn)
            val frame = frames[insnIndex]
            frame.getStack(frame.stackSize - 2).isThis
        } else {
            true
        }

        val count = set.getOrDefault(member, FieldWriteCount.NEVER)
        return when {
            isThis && count == FieldWriteCount.NEVER -> set.plus(Pair(member, FieldWriteCount.EXACTLY_ONCE))
            isThis && count == FieldWriteCount.EXACTLY_ONCE -> set.plus(Pair(member, FieldWriteCount.ONCE_OR_MORE))
            // save an allocation if count is already set to UNKNOWN
            count == FieldWriteCount.ONCE_OR_MORE -> set
            else -> set.plus(Pair(member, FieldWriteCount.ONCE_OR_MORE))
        }
    }
}
