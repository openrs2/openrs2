package org.openrs2.protocol.login.upstream

import io.netty.buffer.ByteBuf
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.openrs2.buffer.readString
import org.openrs2.buffer.use
import org.openrs2.buffer.writeString
import org.openrs2.crypto.Rsa
import org.openrs2.crypto.SymmetricKey
import org.openrs2.crypto.rsa
import org.openrs2.protocol.common.AntiAliasingMode
import org.openrs2.protocol.common.DisplayMode
import org.openrs2.protocol.common.Uid
import org.openrs2.util.Base37

public data class GameLoginPayload(
    val build: Int,
    val advertSuppressed: Boolean,
    val clientSigned: Boolean,
    val displayMode: DisplayMode,
    val canvasWidth: Int,
    val canvasHeight: Int,
    val antiAliasingMode: AntiAliasingMode,
    val uid: Uid,
    val siteSettings: String,
    val affiliate: Int,
    val detailOptions: Int,
    val verifyId: Int,
    val js5ArchiveChecksums: List<Int>,
    // TODO(gpe): XteaKey needs a better name, as it represents an ISAAC key here
    val key: SymmetricKey,
    val username: String,
    val password: String,
) {
    init {
        require(js5ArchiveChecksums.size == JS5_ARCHIVES)
    }

    internal fun write(buf: ByteBuf, rsaKey: RSAKeyParameters) {
        buf.writeInt(build)
        buf.writeByte(0)
        buf.writeBoolean(advertSuppressed)
        buf.writeBoolean(clientSigned)
        buf.writeByte(displayMode.ordinal)
        buf.writeShort(canvasWidth)
        buf.writeShort(canvasHeight)
        buf.writeByte(antiAliasingMode.ordinal)
        uid.write(buf)
        buf.writeString(siteSettings)
        buf.writeInt(affiliate)
        buf.writeInt(detailOptions)
        buf.writeShort(verifyId)

        for (checksum in js5ArchiveChecksums) {
            buf.writeInt(checksum)
        }

        buf.alloc().buffer().use { plaintext ->
            plaintext.writeByte(Rsa.MAGIC)
            plaintext.writeInt(key.k0)
            plaintext.writeInt(key.k1)
            plaintext.writeInt(key.k2)
            plaintext.writeInt(key.k3)
            plaintext.writeLong(Base37.encode(username))
            plaintext.writeString(password)

            plaintext.rsa(rsaKey).use { ciphertext ->
                buf.writeByte(ciphertext.readableBytes())
                buf.writeBytes(ciphertext)
            }
        }
    }

    public companion object {
        public const val JS5_ARCHIVES: Int = 29

        internal fun read(buf: ByteBuf, rsaKey: RSAKeyParameters): GameLoginPayload {
            val build = buf.readInt()

            require(buf.readUnsignedByte().toInt() == 0) {
                "Unknown byte is non-zero"
            }

            val advertSuppressed = buf.readBoolean()
            val clientSigned = buf.readBoolean()

            val displayMode = DisplayMode.fromOrdinal(buf.readUnsignedByte().toInt())
                ?: throw IllegalArgumentException("Invalid DisplayMode")

            val canvasWidth = buf.readUnsignedShort()
            val canvasHeight = buf.readUnsignedShort()

            val antiAliasingMode = AntiAliasingMode.fromOrdinal(buf.readUnsignedByte().toInt())
                ?: throw IllegalArgumentException("Invalid AntiAliasingMode")

            val uid = Uid.read(buf)
            val siteSettings = buf.readString()
            val affiliate = buf.readInt()
            val detailOptions = buf.readInt()
            val verifyId = buf.readUnsignedShort()

            val js5ArchiveChecksums = mutableListOf<Int>()
            for (i in 0 until JS5_ARCHIVES) {
                js5ArchiveChecksums += buf.readInt()
            }

            val ciphertextLen = buf.readUnsignedByte().toInt()
            val ciphertext = buf.readSlice(ciphertextLen)

            ciphertext.rsa(rsaKey).use { plaintext ->
                require(plaintext.readUnsignedByte().toInt() == Rsa.MAGIC) {
                    "Invalid RSA magic"
                }

                val k0 = plaintext.readInt()
                val k1 = plaintext.readInt()
                val k2 = plaintext.readInt()
                val k3 = plaintext.readInt()
                val username = plaintext.readLong()
                val password = plaintext.readString()

                return GameLoginPayload(
                    build,
                    advertSuppressed,
                    clientSigned,
                    displayMode,
                    canvasWidth,
                    canvasHeight,
                    antiAliasingMode,
                    uid,
                    siteSettings,
                    affiliate,
                    detailOptions,
                    verifyId,
                    js5ArchiveChecksums,
                    SymmetricKey(k0, k1, k2, k3),
                    Base37.decodeLowerCase(username),
                    password,
                )
            }
        }
    }
}
