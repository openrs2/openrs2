package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator

public abstract class PacketCodec<T : Packet>(
    public val type: Class<T>,
    public val opcode: Int,
    public val length: Int
) {
    init {
        require(opcode in 0 until 256)
        require(length >= PacketLength.VARIABLE_SHORT)
    }

    public abstract fun decode(input: ByteBuf): T
    public abstract fun encode(input: T, output: ByteBuf)

    public open fun getLength(input: T): Int {
        return length
    }

    public fun allocateBuffer(alloc: ByteBufAllocator, input: T, preferDirect: Boolean): ByteBuf {
        val payloadLen = getLength(input)
        if (payloadLen < 0) {
            return if (preferDirect) {
                alloc.ioBuffer()
            } else {
                alloc.heapBuffer()
            }
        }

        val headerLen = when (length) {
            PacketLength.VARIABLE_BYTE -> 2
            PacketLength.VARIABLE_SHORT -> 3
            else -> 1
        }

        val totalLen = headerLen + payloadLen

        return if (preferDirect) {
            alloc.ioBuffer(totalLen, totalLen)
        } else {
            alloc.heapBuffer(totalLen, totalLen)
        }
    }
}
