package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.openrs2.crypto.StreamCipher

public abstract class PacketCodec<T : Packet>(
    public val type: Class<T>,
    public val opcode: Int
) {
    init {
        require(opcode in 0 until 256)
    }

    public abstract fun decode(input: ByteBuf, cipher: StreamCipher): T
    public abstract fun encode(input: T, output: ByteBuf, cipher: StreamCipher)

    public abstract fun isLengthReadable(input: ByteBuf): Boolean
    public abstract fun readLength(input: ByteBuf): Int

    public abstract fun writeLengthPlaceholder(output: ByteBuf)
    public abstract fun setLength(output: ByteBuf, index: Int, written: Int)

    public open fun allocateBuffer(alloc: ByteBufAllocator, input: T, preferDirect: Boolean): ByteBuf {
        return if (preferDirect) {
            alloc.ioBuffer()
        } else {
            alloc.heapBuffer()
        }
    }
}
