package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec
import javax.inject.Singleton

@Singleton
public class InitGameConnectionCodec : FixedPacketCodec<LoginRequest.InitGameConnection>(
    type = LoginRequest.InitGameConnection::class.java,
    opcode = 14,
    length = 1
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.InitGameConnection {
        val usernameHash = input.readUnsignedByte().toInt()
        return LoginRequest.InitGameConnection(usernameHash)
    }

    override fun encode(input: LoginRequest.InitGameConnection, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(input.usernameHash)
    }
}
