package org.openrs2.protocol

import io.netty.buffer.ByteBuf

internal object VariableShortOptimisedPacketCodec : PacketCodec<VariableShortOptimisedPacket>(
    type = VariableShortOptimisedPacket::class.java,
    opcode = 4,
    length = PacketLength.VARIABLE_SHORT
) {
    override fun decode(input: ByteBuf): VariableShortOptimisedPacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableShortOptimisedPacket(value)
    }

    override fun encode(input: VariableShortOptimisedPacket, output: ByteBuf) {
        output.writeBytes(input.value)
    }

    override fun getLength(input: VariableShortOptimisedPacket): Int {
        return input.value.size
    }
}
