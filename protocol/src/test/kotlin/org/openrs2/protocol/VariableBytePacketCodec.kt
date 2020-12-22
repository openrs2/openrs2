package org.openrs2.protocol

import io.netty.buffer.ByteBuf

internal object VariableBytePacketCodec : PacketCodec<VariableBytePacket>(
    type = VariableBytePacket::class.java,
    opcode = 1,
    length = PacketLength.VARIABLE_BYTE
) {
    override fun decode(input: ByteBuf): VariableBytePacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableBytePacket(value)
    }

    override fun encode(input: VariableBytePacket, output: ByteBuf) {
        output.writeBytes(input.value)
    }
}
