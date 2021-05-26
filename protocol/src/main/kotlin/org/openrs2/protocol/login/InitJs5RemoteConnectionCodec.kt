package org.openrs2.protocol.login

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.PacketCodec

public object InitJs5RemoteConnectionCodec : PacketCodec<LoginRequest.InitJs5RemoteConnection>(
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
