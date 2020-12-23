package org.openrs2.protocol.login

import io.netty.buffer.ByteBuf
import org.openrs2.protocol.PacketCodec

public object InitJs5RemoteConnectionCodec : PacketCodec<LoginRequest.InitJs5RemoteConnection>(
    type = LoginRequest.InitJs5RemoteConnection::class.java,
    opcode = 15,
    length = 4
) {
    override fun decode(input: ByteBuf): LoginRequest.InitJs5RemoteConnection {
        val version = input.readInt()
        return LoginRequest.InitJs5RemoteConnection(version)
    }

    override fun encode(input: LoginRequest.InitJs5RemoteConnection, output: ByteBuf) {
        output.writeInt(input.version)
    }
}
