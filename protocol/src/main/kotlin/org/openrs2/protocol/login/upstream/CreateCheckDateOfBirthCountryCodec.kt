package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import org.openrs2.crypto.StreamCipher
import org.openrs2.protocol.PacketCodec
import java.time.LocalDate
import javax.inject.Singleton

@Singleton
public class CreateCheckDateOfBirthCountryCodec : PacketCodec<LoginRequest.CreateCheckDateOfBirthCountry>(
    type = LoginRequest.CreateCheckDateOfBirthCountry::class.java,
    opcode = 20,
    length = 6
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.CreateCheckDateOfBirthCountry {
        val day = input.readUnsignedByte().toInt()
        val month = input.readUnsignedByte().toInt() + 1
        val year = input.readUnsignedShort()
        val country = input.readUnsignedShort()

        return LoginRequest.CreateCheckDateOfBirthCountry(LocalDate.of(year, month, day), country)
    }

    override fun encode(input: LoginRequest.CreateCheckDateOfBirthCountry, output: ByteBuf, cipher: StreamCipher) {
        output.writeByte(input.dateOfBirth.dayOfMonth)
        output.writeByte(input.dateOfBirth.monthValue - 1)
        output.writeShort(input.dateOfBirth.year)
        output.writeShort(input.country)
    }
}
