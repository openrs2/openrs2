package org.openrs2.cache.config.varp

import io.netty.buffer.ByteBuf
import org.openrs2.cache.config.ConfigType

public class VarpType(id: Int) : ConfigType(id) {
    public var clientCode: Int = 0

    override fun read(buf: ByteBuf, code: Int) {
        when (code) {
            5 -> clientCode = buf.readUnsignedShort()
            else -> throw IllegalArgumentException("Unsupported config code: $code")
        }
    }

    override fun write(buf: ByteBuf) {
        if (clientCode != 0) {
            buf.writeByte(5)
            buf.writeShort(clientCode)
        }

        buf.writeByte(0)
    }
}
