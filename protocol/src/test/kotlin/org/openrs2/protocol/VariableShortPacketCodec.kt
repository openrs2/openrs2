package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher

internal object VariableShortPacketCodec : PacketCodec<VariableShortPacket>(
    type = VariableShortPacket::class.java,
    opcode = 2,
    length = PacketLength.VARIABLE_SHORT
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): VariableShortPacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableShortPacket(value)
    }

    override fun encode(input: VariableShortPacket, output: ByteBuf, cipher: StreamCipher) {
        output.writeBytes(input.value)
    }
}
