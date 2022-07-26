package org.openrs2.protocol.login.downstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec
import javax.inject.Singleton

@Singleton
public class SwitchWorldCodec : FixedPacketCodec<LoginResponse.SwitchWorld>(
    type = LoginResponse.SwitchWorld::class.java,
    opcode = 101,
    length = 2
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginResponse.SwitchWorld {
        val id = input.readUnsignedShort()
        return LoginResponse.SwitchWorld(id)
    }

    override fun encode(input: LoginResponse.SwitchWorld, output: ByteBuf, cipher: StreamCipher) {
        output.writeShort(input.id)
    }
}
