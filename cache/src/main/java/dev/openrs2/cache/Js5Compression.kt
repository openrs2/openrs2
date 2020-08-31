package dev.openrs2.cache

import dev.openrs2.buffer.use
import dev.openrs2.crypto.XteaKey
import dev.openrs2.crypto.xteaDecrypt
import dev.openrs2.crypto.xteaEncrypt
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import java.io.EOFException

public object Js5Compression {
    public fun compress(input: ByteBuf, type: Js5CompressionType, key: XteaKey = XteaKey.ZERO): ByteBuf {
        input.alloc().buffer().use { output ->
            output.writeByte(type.ordinal)

            if (type == Js5CompressionType.NONE) {
                val len = input.readableBytes()
                output.writeInt(len)
                output.writeBytes(input)

                if (!key.isZero) {
                    output.xteaEncrypt(5, len, key)
                }

                return output.retain()
            }

            val lenIndex = output.writerIndex()
            output.writeZero(4)

            val uncompressedLen = input.readableBytes()
            output.writeInt(uncompressedLen)

            val start = output.writerIndex()

            type.createOutputStream(ByteBufOutputStream(output)).use { outputStream ->
                input.readBytes(outputStream, uncompressedLen)
            }

            val len = output.writerIndex() - start
            output.setInt(lenIndex, len)

            if (!key.isZero) {
                output.xteaEncrypt(5, len + 4, key)
            }

            return output.retain()
        }
    }

    public fun compressBest(
        input: ByteBuf,
        enableLzma: Boolean = false,
        enableUncompressedEncryption: Boolean = false,
        key: XteaKey = XteaKey.ZERO
    ): ByteBuf {
        val types = mutableListOf(Js5CompressionType.BZIP2, Js5CompressionType.GZIP)
        if (enableLzma) {
            types += Js5CompressionType.LZMA
        }
        if (enableUncompressedEncryption || key.isZero) {
            /*
             * The 550 client doesn't strip the 2 byte version trailer before
             * passing a group to the XTEA decryption function. This causes the
             * last block to be incorrectly decrypt in many cases (depending
             * on the length of the group mod the XTEA block size).
             *
             * This doesn't cause any problems with the client's GZIP/BZIP2
             * implementations, as the last block is always part of the trailer
             * and the trailer isn't checked. However, it would corrupt the
             * last block of an unencrypted group.
             *
             * TODO(gpe): are there any clients with LZMA support _and_ the
             * decryption bug? Could the enableLzma flag be re-used for
             * enableNoneWithKey? Or should LZMA also be disabled in clients
             * with the decryption bug?
             */
            types += Js5CompressionType.NONE
        }

        var best = compress(input.slice(), types.first(), key)
        try {
            for (type in types.drop(1)) {
                compress(input.slice(), type, key).use { output ->
                    if (output.readableBytes() < best.readableBytes()) {
                        best.release()
                        best = output.retain()
                    }
                }
            }

            // consume all of input so this method is a drop-in replacement for compress()
            input.skipBytes(input.readableBytes())

            return best.retain()
        } finally {
            best.release()
        }
    }

    public fun uncompress(input: ByteBuf, key: XteaKey = XteaKey.ZERO): ByteBuf {
        val typeId = input.readUnsignedByte().toInt()
        val type = Js5CompressionType.fromOrdinal(typeId)
        require(type != null) {
            "Invalid compression type: $typeId"
        }

        val len = input.readInt()
        require(len >= 0) {
            "Length is negative: $len"
        }

        if (type == Js5CompressionType.NONE) {
            input.readBytes(len).use { output ->
                if (!key.isZero) {
                    output.xteaDecrypt(0, len, key)
                }
                return output.retain()
            }
        }

        decrypt(input, len + 4, key).use { plaintext ->
            val uncompressedLen = plaintext.readInt()
            require(uncompressedLen >= 0) {
                "Uncompressed length is negative: $uncompressedLen"
            }

            plaintext.alloc().buffer(uncompressedLen, uncompressedLen).use { output ->
                type.createInputStream(ByteBufInputStream(plaintext, len), uncompressedLen).use { inputStream ->
                    var remaining = uncompressedLen
                    while (remaining > 0) {
                        val n = output.writeBytes(inputStream, remaining)
                        if (n == -1) {
                            throw EOFException()
                        }
                        remaining -= n
                    }
                }

                return output.retain()
            }
        }
    }

    private fun decrypt(buf: ByteBuf, len: Int, key: XteaKey): ByteBuf {
        if (key.isZero) {
            return buf.readRetainedSlice(len)
        }

        buf.readBytes(len).use { output ->
            output.xteaDecrypt(0, len, key)
            return output.retain()
        }
    }
}
