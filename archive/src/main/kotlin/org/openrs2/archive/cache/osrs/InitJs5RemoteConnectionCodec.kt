package org.openrs2.archive.cache.osrs

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.crypto.SymmetricKey
import org.openrs2.protocol.FixedPacketCodec

public object InitJs5RemoteConnectionCodec : FixedPacketCodec<InitJs5RemoteConnection>(
    type = InitJs5RemoteConnection::class.java,
    opcode = 15,
    length = 20
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): InitJs5RemoteConnection {
        val build = input.readInt()
        val k0 = input.readInt()
        val k1 = input.readInt()
        val k2 = input.readInt()
        val k3 = input.readInt()
        return InitJs5RemoteConnection(build, SymmetricKey(k0, k1, k2, k3))
    }

    override fun encode(input: InitJs5RemoteConnection, output: ByteBuf, cipher: StreamCipher) {
        output.writeInt(input.build)
        output.writeInt(input.key.k0)
        output.writeInt(input.key.k1)
        output.writeInt(input.key.k2)
        output.writeInt(input.key.k3)
    }
}
