package org.openrs2.archive.cache.nxt

import io.netty.buffer.ByteBuf
import org.openrs2.buffer.readString
import org.openrs2.buffer.writeString
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.PacketCodec
import org.openrs2.protocol.PacketLength

public object InitJs5RemoteConnectionCodec : PacketCodec<InitJs5RemoteConnection>(
    length = PacketLength.VARIABLE_BYTE,
    opcode = 15,
    type = InitJs5RemoteConnection::class.java
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): InitJs5RemoteConnection {
        val buildMajor = input.readInt()
        val buildMinor = input.readInt()
        val token = input.readString()
        val language = input.readUnsignedByte().toInt()
        return InitJs5RemoteConnection(buildMajor, buildMinor, token, language)
    }

    override fun encode(input: InitJs5RemoteConnection, output: ByteBuf, cipher: StreamCipher) {
        output.writeInt(input.buildMajor)
        output.writeInt(input.buildMinor)
        output.writeString(input.token)
        output.writeByte(input.language)
    }
}
