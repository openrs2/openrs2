package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher

internal object VariableByteOptimisedPacketCodec : VariableBytePacketCodec<VariableByteOptimisedPacket>(
    type = VariableByteOptimisedPacket::class.java,
    opcode = 3
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): VariableByteOptimisedPacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableByteOptimisedPacket(value)
    }

    override fun encode(input: VariableByteOptimisedPacket, output: ByteBuf, cipher: StreamCipher) {
        output.writeBytes(input.value)
    }

    override fun getLength(input: VariableByteOptimisedPacket): Int {
        return input.value.size
    }
}
