package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters
import org.openrs2.buffer.readString
import org.openrs2.buffer.use
import org.openrs2.buffer.writeString
import org.openrs2.crypto.Rsa
import org.openrs2.crypto.StreamCipher
import org.openrs2.crypto.publicKey
import org.openrs2.crypto.rsa
import org.openrs2.crypto.secureRandom
import org.openrs2.protocol.VariableBytePacketCodec
import org.openrs2.util.Base37
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CheckWorldSuitabilityCodec @Inject constructor(
    private val key: RSAPrivateCrtKeyParameters
) : VariableBytePacketCodec<LoginRequest.CheckWorldSuitability>(
    type = LoginRequest.CheckWorldSuitability::class.java,
    opcode = 24
) {
    override fun decode(input: ByteBuf, cipher: StreamCipher): LoginRequest.CheckWorldSuitability {
        val build = input.readShort().toInt()

        val ciphertextLen = input.readUnsignedByte().toInt()
        val ciphertext = input.readSlice(ciphertextLen)

        ciphertext.rsa(key).use { plaintext ->
            require(plaintext.readUnsignedByte().toInt() == Rsa.MAGIC) {
                "Invalid RSA magic"
            }
            plaintext.skipBytes(4)

            val username = Base37.decodeLowerCase(plaintext.readLong())
            plaintext.skipBytes(4)

            val password = plaintext.readString()
            plaintext.skipBytes(4)

            return LoginRequest.CheckWorldSuitability(build, username, password)
        }
    }

    override fun encode(input: LoginRequest.CheckWorldSuitability, output: ByteBuf, cipher: StreamCipher) {
        output.writeShort(input.build)

        output.alloc().buffer().use { plaintext ->
            plaintext.writeByte(Rsa.MAGIC)
            plaintext.writeInt(secureRandom.nextInt())

            plaintext.writeLong(Base37.encode(input.username))
            plaintext.writeInt(secureRandom.nextInt())

            plaintext.writeString(input.password)
            plaintext.writeInt(secureRandom.nextInt())

            plaintext.rsa(key.publicKey).use { ciphertext ->
                output.writeByte(ciphertext.readableBytes())
                output.writeBytes(ciphertext)
            }
        }
    }
}
