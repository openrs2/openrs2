package org.openrs2.cache.config.inv

import io.netty.buffer.ByteBuf
import org.openrs2.cache.config.ConfigType

public class InvType(id: Int) : ConfigType(id) {
    public var size: Int = 0

    override fun read(buf: ByteBuf, code: Int) {
        when (code) {
            2 -> size = buf.readUnsignedShort()
            else -> throw IllegalArgumentException("Unsupported config code: $code")
        }
    }

    override fun write(buf: ByteBuf) {
        if (size != 0) {
            buf.writeByte(2)
            buf.writeShort(size)
        }

        buf.writeByte(0)
    }
}
