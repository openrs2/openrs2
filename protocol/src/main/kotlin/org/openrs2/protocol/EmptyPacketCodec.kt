package org.openrs2.protocol

import io.netty.buffer.ByteBuf

public abstract class EmptyPacketCodec<T : Packet>(
    private val packet: T,
    opcode: Int
) : PacketCodec<T>(packet.javaClass, opcode, length = 0) {
    override fun decode(input: ByteBuf): T {
        return packet
    }

    override fun encode(input: T, output: ByteBuf) {
        // empty
    }
}
