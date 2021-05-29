package org.openrs2.cache.config

import io.netty.buffer.ByteBuf

public abstract class ConfigType(
    public val id: Int
) {
    public abstract fun read(buf: ByteBuf, code: Int)
    public abstract fun write(buf: ByteBuf)

    public fun read(buf: ByteBuf) {
        while (true) {
            val code = buf.readUnsignedByte().toInt()
            if (code == 0) {
                break
            }

            read(buf, code)
        }
    }
}
