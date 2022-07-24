package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.EncoderException

public abstract class VariableBytePacketCodec<T : Packet>(
    type: Class<T>,
    opcode: Int
) : PacketCodec<T>(type, opcode) {
    override fun isLengthReadable(input: ByteBuf): Boolean {
        return input.isReadable
    }

    override fun readLength(input: ByteBuf): Int {
        return input.readUnsignedByte().toInt()
    }

    override fun writeLengthPlaceholder(output: ByteBuf) {
        output.writeZero(1)
    }

    override fun setLength(output: ByteBuf, index: Int, written: Int) {
        if (written >= 256) {
            throw EncoderException("Variable byte payload too long: $written bytes")
        }

        output.setByte(index, written)
    }

    public open fun getLength(input: T): Int {
        return -1
    }

    override fun allocateBuffer(alloc: ByteBufAllocator, input: T, preferDirect: Boolean): ByteBuf {
        val length = getLength(input)
        if (length < 0) {
            return super.allocateBuffer(alloc, input, preferDirect)
        }

        return if (preferDirect) {
            alloc.ioBuffer(2 + length, 2 + length)
        } else {
            alloc.heapBuffer(2 + length, 2 + length)
        }
    }
}
