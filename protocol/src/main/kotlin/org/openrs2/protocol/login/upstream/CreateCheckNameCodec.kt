package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec
import org.openrs2.util.Base37
import javax.inject.Singleton

@Singleton
public class CreateCheckNameCodec : FixedPacketCodec<LoginRequest.CreateCheckName>(
    type = LoginRequest.CreateCheckName::class.java,
    opcode = 21,
    length = 8
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.CreateCheckName {
        val username = Base37.decodeLowerCase(input.readLong())
        return LoginRequest.CreateCheckName(username)
    }

    override fun encode(input: LoginRequest.CreateCheckName, output: ByteBuf, cipher: StreamCipher) {
        output.writeLong(Base37.encode(input.username))
    }
}
