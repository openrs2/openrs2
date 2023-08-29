package org.openrs2.protocol.login.downstream

import io.netty.buffer.ByteBuf
import jakarta.inject.Singleton
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec

@Singleton
public class DisallowedByScriptCodec : FixedPacketCodec<LoginResponse.DisallowedByScript>(
    type = LoginResponse.DisallowedByScript::class.java,
    opcode = 29,
    length = 1
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginResponse.DisallowedByScript {
        val reason = input.readUnsignedByte().toInt()
        return LoginResponse.DisallowedByScript(reason)
    }

    override fun encode(input: LoginResponse.DisallowedByScript, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(input.reason)
    }
}
