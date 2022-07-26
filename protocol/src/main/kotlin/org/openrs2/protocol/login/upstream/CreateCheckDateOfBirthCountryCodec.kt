package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.FixedPacketCodec
import javax.inject.Singleton

@Singleton
public class CreateCheckDateOfBirthCountryCodec : FixedPacketCodec<LoginRequest.CreateCheckDateOfBirthCountry>(
    type = LoginRequest.CreateCheckDateOfBirthCountry::class.java,
    opcode = 20,
    length = 6
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.CreateCheckDateOfBirthCountry {
        val day = input.readUnsignedByte().toInt()
        val month = input.readUnsignedByte().toInt()
        val year = input.readUnsignedShort()
        val country = input.readUnsignedShort()

        return LoginRequest.CreateCheckDateOfBirthCountry(year, month, day, country)
    }

    override fun encode(input: LoginRequest.CreateCheckDateOfBirthCountry, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(input.day)
        output.writeByte(input.month)
        output.writeShort(input.year)
        output.writeShort(input.country)
    }
}
