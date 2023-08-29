package org.openrs2.protocol.login.downstream

import io.netty.buffer.ByteBuf
import jakarta.inject.Singleton
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec

@Singleton
public class HopBlockedCodec : FixedPacketCodec<LoginResponse.HopBlocked>(
    type = LoginResponse.HopBlocked::class.java,
    opcode = 21,
    length = 1
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginResponse.HopBlocked {
        val time = input.readUnsignedByte().toInt()
        return LoginResponse.HopBlocked(time)
    }

    override fun encode(input: LoginResponse.HopBlocked, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(input.time)
    }
}
