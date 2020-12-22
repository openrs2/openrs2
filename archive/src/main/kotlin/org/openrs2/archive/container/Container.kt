package org.openrs2.archive.container

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import io.netty.buffer.DefaultByteBufHolder
import org.openrs2.buffer.crc32
import org.openrs2.crypto.Whirlpool

public abstract class Container(
    data: ByteBuf
) : DefaultByteBufHolder(data) {
    public val bytes: ByteArray = ByteBufUtil.getBytes(data, data.readerIndex(), data.readableBytes(), false)
    public val crc32: Int = data.crc32()
    public val whirlpool: ByteArray = Whirlpool.whirlpool(bytes)
    public abstract val encrypted: Boolean
}
