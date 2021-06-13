package org.openrs2.protocol.login

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.PacketCodec

public object InitGameConnectionCodec : PacketCodec<LoginRequest.InitGameConnection>(
    opcode = 14,
    length = 1,
    type = LoginRequest.InitGameConnection::class.java
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.InitGameConnection {
        val usernameHash = input.readUnsignedByte().toInt()
        return LoginRequest.InitGameConnection(usernameHash)
    }

    override fun encode(input: LoginRequest.InitGameConnection, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(input.usernameHash)
    }
}
