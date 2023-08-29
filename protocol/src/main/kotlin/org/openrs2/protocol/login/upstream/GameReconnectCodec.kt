package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.VariableShortPacketCodec

@Singleton
public class GameReconnectCodec @Inject constructor(
    private val key: RSAPrivateCrtKeyParameters,
) : VariableShortPacketCodec<LoginRequest.GameReconnect>(
    type = LoginRequest.GameReconnect::class.java,
    opcode = 18,
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.GameReconnect {
        return LoginRequest.GameReconnect(GameLoginPayload.read(input, key))
    }

    override fun encode(input: LoginRequest.GameReconnect, output: ByteBuf, cipher: StreamCipher) {
        input.payload.write(output, key)
    }
}
