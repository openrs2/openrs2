package org.openrs2.archive.cache.nxt

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec

public object Js5OkCodec : FixedPacketCodec<LoginResponse.Js5Ok>(
    type = LoginResponse.Js5Ok::class.java,
    opcode = 0,
    length = LoginResponse.Js5Ok.LOADING_REQUIREMENTS * 4
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginResponse.Js5Ok {
        val loadingRequirements = mutableListOf<Int>()
        for (i in 0 until LoginResponse.Js5Ok.LOADING_REQUIREMENTS) {
            loadingRequirements += input.readInt()
        }
        return LoginResponse.Js5Ok(loadingRequirements)
    }

    override fun encode(input: LoginResponse.Js5Ok, output: ByteBuf, cipher: StreamCipher) {
        for (requirement in input.loadingRequirements) {
            output.writeInt(requirement)
        }
    }
}
