package org.openrs2.protocol.login

import io.netty.buffer.ByteBuf
import org.openrs2.protocol.PacketCodec

public object RequestWorldListCodec : PacketCodec<LoginRequest.RequestWorldList>(
    type = LoginRequest.RequestWorldList::class.java,
    opcode = 23,
    length = 4
) {
    override fun decode(input: ByteBuf): LoginRequest.RequestWorldList {
        val checksum = input.readInt()
        return LoginRequest.RequestWorldList(checksum)
    }

    override fun encode(input: LoginRequest.RequestWorldList, output: ByteBuf) {
        output.writeInt(input.checksum)
    }
}
