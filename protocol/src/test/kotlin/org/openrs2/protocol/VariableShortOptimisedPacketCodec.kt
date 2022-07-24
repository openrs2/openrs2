package org.openrs2.protocol

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher

internal object VariableShortOptimisedPacketCodec : VariableShortPacketCodec<VariableShortOptimisedPacket>(
    type = VariableShortOptimisedPacket::class.java,
    opcode = 4
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): VariableShortOptimisedPacket {
        val value = ByteArray(input.readableBytes())
        input.readBytes(value)
        return VariableShortOptimisedPacket(value)
    }

    override fun encode(input: VariableShortOptimisedPacket, output: ByteBuf, cipher: StreamCipher) {
        output.writeBytes(input.value)
    }

    override fun getLength(input: VariableShortOptimisedPacket): Int {
        return input.value.size
    }
}
