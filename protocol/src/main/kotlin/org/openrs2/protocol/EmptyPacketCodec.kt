package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher

public abstract class EmptyPacketCodec<T : Packet>(
    private val packet: T,
    opcode: Int
) : FixedPacketCodec<T>(packet.javaClass, opcode, length = 0) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): T {
        return packet
    }

    override fun encode(input: T, output: ByteBuf, cipher: StreamCipher) {
        // empty
    }
}
