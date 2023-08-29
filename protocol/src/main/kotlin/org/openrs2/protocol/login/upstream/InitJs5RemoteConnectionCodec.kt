package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import jakarta.inject.Singleton
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec

@Singleton
public class InitJs5RemoteConnectionCodec : FixedPacketCodec<LoginRequest.InitJs5RemoteConnection>(
    type = LoginRequest.InitJs5RemoteConnection::class.java,
    opcode = 15,
    length = 4
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.InitJs5RemoteConnection {
        val build = input.readInt()
        return LoginRequest.InitJs5RemoteConnection(build)
    }

    override fun encode(input: LoginRequest.InitJs5RemoteConnection, output: ByteBuf, cipher: StreamCipher) {
        output.writeInt(input.build)
    }
}
