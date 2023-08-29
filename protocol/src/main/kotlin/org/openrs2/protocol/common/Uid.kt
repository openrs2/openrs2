package org.openrs2.protocol.common

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil

public class Uid private constructor(
    private val bytes: ByteArray,
) {
    init {
        require(bytes.size == LENGTH)
    }

    public fun write(buf: ByteBuf) {
        buf.writeBytes(bytes)
    }

    public override fun toString(): String {
        return ByteBufUtil.hexDump(bytes)
    }

    public companion object {
        public const val LENGTH: Int = 24

        public fun read(buf: ByteBuf): Uid {
            val bytes = ByteArray(LENGTH)
            buf.readBytes(bytes)
            return Uid(bytes)
        }
    }
}
