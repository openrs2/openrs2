package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.VariableShortPacketCodec

@Singleton
public class GameLoginCodec @Inject constructor(
    private val key: RSAPrivateCrtKeyParameters,
) : VariableShortPacketCodec<LoginRequest.GameLogin>(
    type = LoginRequest.GameLogin::class.java,
    opcode = 16,
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.GameLogin {
        return LoginRequest.GameLogin(GameLoginPayload.read(input, key))
    }

    override fun encode(input: LoginRequest.GameLogin, output: ByteBuf, cipher: StreamCipher) {
        input.payload.write(output, key)
    }
}
