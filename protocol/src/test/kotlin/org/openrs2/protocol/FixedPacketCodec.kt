package org.openrs2.protocol

import io.netty.buffer.ByteBuf

internal object FixedPacketCodec : PacketCodec<FixedPacket>(
    type = FixedPacket::class.java,
    opcode = 0,
    length = 4
) {
    override fun decode(input: ByteBuf): FixedPacket {
        val value = input.readInt()
        return FixedPacket(value)
    }

    override fun encode(input: FixedPacket, output: ByteBuf) {
        output.writeInt(input.value)
    }
}
