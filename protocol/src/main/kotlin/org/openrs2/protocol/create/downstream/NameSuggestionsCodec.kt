package org.openrs2.protocol.create.downstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.PacketCodec
import org.openrs2.util.Base37

public class NameSuggestionsCodec : PacketCodec<CreateResponse.NameSuggestions>(
    type = CreateResponse.NameSuggestions::class.java,
    opcode = 21
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): CreateResponse.NameSuggestions {
        val names = mutableListOf<String>()
        while (input.readableBytes() >= 8) {
            names += Base37.decodeLowerCase(input.readLong())
        }
        return CreateResponse.NameSuggestions(names)
    }

    override fun encode(input: CreateResponse.NameSuggestions, output: ByteBuf, cipher: StreamCipher) {
        for (name in input.names) {
            output.writeLong(Base37.encode(name))
        }
    }

    override fun isLengthReadable(input: ByteBuf): Boolean {
        return input.isReadable
    }

    override fun readLength(input: ByteBuf): Int {
        return input.readUnsignedByte().toInt() * 8
    }

    override fun writeLengthPlaceholder(output: ByteBuf) {
        output.writeZero(1)
    }

    override fun setLength(output: ByteBuf, index: Int, written: Int) {
        require(written % 8 != 0) {
            "Name suggestions payload length must be a multiple of 8"
        }

        output.setByte(index, written / 8)
    }
}
