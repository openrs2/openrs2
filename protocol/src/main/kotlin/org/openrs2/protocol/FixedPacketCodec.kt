package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.handler.codec.EncoderException

public abstract class FixedPacketCodec<T : Packet>(
    type: Class<T>,
    opcode: Int,
    public val length: Int
) : PacketCodec<T>(type, opcode) {
    override fun isLengthReadable(input: ByteBuf): Boolean {
        return true
    }

    override fun readLength(input: ByteBuf): Int {
        return length
    }

    override fun writeLengthPlaceholder(output: ByteBuf) {
        // empty
    }

    override fun setLength(output: ByteBuf, index: Int, written: Int) {
        if (written != length) {
            throw EncoderException("Fixed payload length mismatch (expected $length bytes, got $written bytes)")
        }
    }

    override fun allocateBuffer(alloc: ByteBufAllocator, input: T, preferDirect: Boolean): ByteBuf {
        return if (preferDirect) {
            alloc.ioBuffer(1 + length, 1 + length)
        } else {
            alloc.heapBuffer(1 + length, 1 + length)
        }
    }
}
