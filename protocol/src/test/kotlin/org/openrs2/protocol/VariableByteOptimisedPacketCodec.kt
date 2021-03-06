package org.openrs2.protocol

import io.netty.buffer.ByteBuf

internal object VariableByteOptimisedPacketCodec : PacketCodec<VariableByteOptimisedPacket>(
    type = VariableByteOptimisedPacket::class.java,
    opcode = 3,
    length = PacketLength.VARIABLE_BYTE
) {
    override fun decode(input: ByteBuf): VariableByteOptimisedPacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableByteOptimisedPacket(value)
    }

    override fun encode(input: VariableByteOptimisedPacket, output: ByteBuf) {
        output.writeBytes(input.value)
    }

    override fun getLength(input: VariableByteOptimisedPacket): Int {
        return input.value.size
    }
}
