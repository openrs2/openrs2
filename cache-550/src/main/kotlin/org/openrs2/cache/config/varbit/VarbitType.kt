package org.openrs2.cache.config.varbit

import io.netty.buffer.ByteBuf
import org.openrs2.cache.config.ConfigType

public class VarbitType(id: Int) : ConfigType(id) {
    public var baseVar: Int = 0
    public var startBit: Int = 0
    public var endBit: Int = 0

    override fun read(buf: ByteBuf, code: Int) {
        when (code) {
            1 -> {
                baseVar = buf.readUnsignedShort()
                startBit = buf.readUnsignedByte().toInt()
                endBit = buf.readUnsignedByte().toInt()
            }
            else -> throw IllegalArgumentException("Unsupported config code: $code")
        }
    }

    override fun write(buf: ByteBuf) {
        if (baseVar != 0 || startBit != 0 || endBit != 0) {
            buf.writeByte(1)
            buf.writeShort(baseVar)
            buf.writeByte(startBit)
            buf.writeByte(endBit)
        }

        buf.writeByte(0)
    }
}
