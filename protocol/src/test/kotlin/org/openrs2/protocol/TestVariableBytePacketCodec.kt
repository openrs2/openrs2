package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher

internal object TestVariableBytePacketCodec : VariableBytePacketCodec<VariableBytePacket>(
    type = VariableBytePacket::class.java,
    opcode = 1
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): VariableBytePacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableBytePacket(value)
    }

    override fun encode(input: VariableBytePacket, output: ByteBuf, cipher: StreamCipher) {
        output.writeBytes(input.value)
    }
}
