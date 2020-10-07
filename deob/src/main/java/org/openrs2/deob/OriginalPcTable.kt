package org.openrs2.deob

import org.objectweb.asm.Attribute
import org.objectweb.asm.ByteVector
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Label
import org.objectweb.asm.tree.LabelNode

public class OriginalPcTable(
    private val pcs: Map<LabelNode, Int>,
    private val names: Map<Int, String>
) : Attribute("OriginalPcTable") {
    override fun isCodeAttribute(): Boolean {
        return true
    }

    override fun isUnknown(): Boolean {
        return false
    }

    override fun getLabels(): Array<Label> {
        return pcs.keys.map(LabelNode::getLabel).toTypedArray()
    }

    override fun write(
        classWriter: ClassWriter,
        code: ByteArray?,
        codeLength: Int,
        maxStack: Int,
        maxLocals: Int
    ): ByteVector {
        val buf = ByteVector()

        buf.putShort(pcs.size)
        for ((label, pc) in pcs) {
            buf.putShort(label.label.offset)
            buf.putShort(pc)
        }

        buf.putShort(names.size)
        for ((pc, name) in names) {
            buf.putShort(pc)
            buf.putShort(classWriter.newUTF8(name))
        }

        return buf
    }
}
