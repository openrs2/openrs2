package org.openrs2.protocol

import io.netty.buffer.ByteBuf

internal object VariableShortPacketCodec : PacketCodec<VariableShortPacket>(
    type = VariableShortPacket::class.java,
    opcode = 2,
    length = PacketLength.VARIABLE_SHORT
) {
    override fun decode(input: ByteBuf): VariableShortPacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableShortPacket(value)
    }

    override fun encode(input: VariableShortPacket, output: ByteBuf) {
        output.writeBytes(input.value)
    }
}
