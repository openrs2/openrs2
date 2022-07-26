package org.openrs2.protocol.login.downstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec
import javax.inject.Singleton

@Singleton
public class ExchangeSessionKeyCodec : FixedPacketCodec<LoginResponse.ExchangeSessionKey>(
    type = LoginResponse.ExchangeSessionKey::class.java,
    opcode = 0,
    length = 8
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginResponse.ExchangeSessionKey {
        val key = input.readLong()
        return LoginResponse.ExchangeSessionKey(key)
    }

    override fun encode(input: LoginResponse.ExchangeSessionKey, output: ByteBuf, cipher: StreamCipher) {
        output.writeLong(input.key)
    }
}
