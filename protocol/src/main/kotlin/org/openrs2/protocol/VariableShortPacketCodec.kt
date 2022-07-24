package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.EncoderException

public abstract class VariableShortPacketCodec<T : Packet>(
    type: Class<T>,
    opcode: Int
) : PacketCodec<T>(type, opcode) {
    override fun isLengthReadable(input: ByteBuf): Boolean {
        return input.readableBytes() >= 2
    }

    override fun readLength(input: ByteBuf): Int {
        return input.readUnsignedShort()
    }

    override fun writeLengthPlaceholder(output: ByteBuf) {
        output.writeZero(2)
    }

    override fun setLength(output: ByteBuf, index: Int, written: Int) {
        if (written >= 65536) {
            throw EncoderException("Variable short payload too long: $written bytes")
        }

        output.setShort(index, written)
    }

    public open fun getLength(input: T): Int {
        return -2
    }

    override fun allocateBuffer(alloc: ByteBufAllocator, input: T, preferDirect: Boolean): ByteBuf {
        val length = getLength(input)
        if (length < 0) {
            return super.allocateBuffer(alloc, input, preferDirect)
        }

        return if (preferDirect) {
            alloc.ioBuffer(3 + length, 3 + length)
        } else {
            alloc.heapBuffer(3 + length, 3 + length)
        }
    }
}
