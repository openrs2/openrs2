package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import org.openrs2.buffer.use
import org.openrs2.crypto.XteaKey
import org.openrs2.crypto.xteaDecrypt
import org.openrs2.crypto.xteaEncrypt
import java.io.IOException

public object Js5Compression {
    private val BZIP2_MAGIC = byteArrayOf(0x31, 0x41, 0x59, 0x26, 0x53, 0x59)

    private const val GZIP_MAGIC = 0x1F8B
    private const val GZIP_COMPRESSION_METHOD_DEFLATE = 0x08

    private const val LZMA_PB_MAX = 4
    private const val LZMA_PRESET_DICT_SIZE_MAX = 1 shl 26

    @JvmOverloads
    public fun compress(input: ByteBuf, type: Js5CompressionType, key: XteaKey = XteaKey.ZERO): ByteBuf {
        input.alloc().buffer().use { output ->
            output.writeByte(type.ordinal)

            if (type == Js5CompressionType.UNCOMPRESSED) {
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
             * enableUncompressedEncryption? Or should LZMA also be disabled in
             * clients with the decryption bug?
             */
            types += Js5CompressionType.UNCOMPRESSED
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

    @JvmOverloads
    public fun uncompress(input: ByteBuf, key: XteaKey = XteaKey.ZERO): ByteBuf {
        if (input.readableBytes() < 5) {
            throw IOException("Missing header")
        }

        val typeId = input.readUnsignedByte().toInt()
        val type = Js5CompressionType.fromOrdinal(typeId)
            ?: throw IOException("Invalid compression type: $typeId")

        val len = input.readInt()
        if (len < 0) {
            throw IOException("Length is negative: $len")
        }

        if (type == Js5CompressionType.UNCOMPRESSED) {
            if (input.readableBytes() < len) {
                throw IOException("Data truncated")
            }

            input.readBytes(len).use { output ->
                if (!key.isZero) {
                    output.xteaDecrypt(0, len, key)
                }
                return output.retain()
            }
        }

        val lenWithUncompressedLen = len + 4
        if (input.readableBytes() < lenWithUncompressedLen) {
            throw IOException("Compressed data truncated")
        }

        decrypt(input, lenWithUncompressedLen, key).use { plaintext ->
            val uncompressedLen = plaintext.readInt()
            if (uncompressedLen < 0) {
                throw IOException("Uncompressed length is negative: $uncompressedLen")
            }

            type.createInputStream(ByteBufInputStream(plaintext, len), uncompressedLen).use { inputStream ->
                plaintext.alloc().buffer(uncompressedLen, uncompressedLen).use { output ->
                    var remaining = uncompressedLen
                    while (remaining > 0) {
                        val n = output.writeBytes(inputStream, remaining)
                        if (n == -1) {
                            throw IOException("Uncompressed data truncated")
                        }
                        remaining -= n
                    }

                    if (inputStream.read() != -1) {
                        throw IOException("Uncompressed data overflow")
                    }

                    return output.retain()
                }
            }
        }
    }

    public fun uncompressUnlessEncrypted(input: ByteBuf): ByteBuf? {
        return uncompressIfKeyValid(input, XteaKey.ZERO)
    }

    public fun uncompressIfKeyValid(input: ByteBuf, key: XteaKey): ByteBuf? {
        if (input.readableBytes() < 5) {
            throw IOException("Missing header")
        }

        val typeId = input.readUnsignedByte().toInt()
        val type = Js5CompressionType.fromOrdinal(typeId)
            ?: throw IOException("Invalid compression type: $typeId")

        val len = input.readInt()
        if (len < 0) {
            throw IOException("Length is negative: $len")
        }

        if (type == Js5CompressionType.UNCOMPRESSED) {
            val n = input.readableBytes()
            if (n < len) {
                throw IOException("Data truncated ($n bytes, expecting $len)")
            }

            /*
             * There is no easy way for us to be sure whether an uncompressed
             * group's key is valid or not, as we'd need specific validation
             * code for each file format the client uses (currently only maps,
             * though apparently RS3 is also capable of encrypting interfaces).
             *
             * In practice, encrypted files don't tend to be uncompressed
             * anyway - in 550, this functionality is actually broken due to a
             * bug (see the comment about enableUncompressedEncryption in
             * compressBest above).
             *
             * We therefore assume all uncompressed groups are unencrypted.
             */
            if (!key.isZero) {
                input.skipBytes(len)
                return null
            }

            return input.readBytes(len)
        }

        val lenWithUncompressedLen = len + 4
        val n = input.readableBytes()
        if (n < lenWithUncompressedLen) {
            throw IOException("Compressed data truncated ($n bytes, expecting $lenWithUncompressedLen)")
        }

        /*
         * Decrypt two XTEA blocks, which is sufficient to quickly check the
         * uncompressed length and the compression algorithm's header in each
         * case:
         *
         * BZIP2: 6 byte block magic
         * GZIP:  2 byte magic, 1 byte compression method
         * LZMA:  1 byte properties, 4 byte dictionary size
         *
         * We never expect to see a file with fewer than two blocks. The
         * compressed length of an empty file is always two XTEA blocks in each
         * case:
         *
         * BZIP2: 10 bytes
         * GZIP:  18 bytes
         * LZMA:  10 bytes
         */
        if (lenWithUncompressedLen < 16) {
            throw IOException("Compressed data shorter than two XTEA blocks")
        }

        decrypt(input.slice(), 16, key).use { plaintext ->
            val uncompressedLen = plaintext.readInt()
            if (uncompressedLen < 0) {
                input.skipBytes(lenWithUncompressedLen)
                return null
            }

            when (type) {
                Js5CompressionType.UNCOMPRESSED -> throw AssertionError()
                Js5CompressionType.BZIP2 -> {
                    val magic = ByteArray(BZIP2_MAGIC.size)
                    plaintext.readBytes(magic)
                    if (!magic.contentEquals(BZIP2_MAGIC)) {
                        input.skipBytes(lenWithUncompressedLen)
                        return null
                    }
                }

                Js5CompressionType.GZIP -> {
                    val magic = plaintext.readUnsignedShort()
                    if (magic != GZIP_MAGIC) {
                        input.skipBytes(lenWithUncompressedLen)
                        return null
                    }

                    // Jagex's implementation only supports DEFLATE.
                    val compressionMethod = plaintext.readUnsignedByte().toInt()
                    if (compressionMethod != GZIP_COMPRESSION_METHOD_DEFLATE) {
                        input.skipBytes(lenWithUncompressedLen)
                        return null
                    }
                }

                Js5CompressionType.LZMA -> {
                    val properties = plaintext.readUnsignedByte()

                    /*
                     * The encoding of the properties byte means it isn't
                     * possible for lc/lp to be out of range.
                     */

                    val pb = properties / 45
                    if (pb > LZMA_PB_MAX) {
                        input.skipBytes(lenWithUncompressedLen)
                        return null
                    }

                    /*
                     * Jagex's implementation doesn't support dictionary sizes
                     * greater than 2 GiB (which are less than zero when the
                     * dictionary size is read as a signed integer).
                     *
                     * We also assume dictionary sizes larger than 64 MiB (the
                     * size of the dictionary at -9, the highest preset level)
                     * indicate the key is incorrect.
                     *
                     * In theory these dictionary sizes are actually valid.
                     * However, in practice Jagex seems to only use level -3 or
                     * -4 (as their dictionary size is 4 MiB).
                     *
                     * Attempting to decompress LZMA streams with larger
                     * dictionaries can easily cause OOM exceptions in low
                     * memory environments.
                     */
                    val dictSize = plaintext.readIntLE()
                    if (dictSize < 0) {
                        input.skipBytes(lenWithUncompressedLen)
                        return null
                    } else if (dictSize > LZMA_PRESET_DICT_SIZE_MAX) {
                        input.skipBytes(lenWithUncompressedLen)
                        return null
                    }
                }
            }
        }

        // Run the entire decompression algorithm to confirm the key is valid.
        decrypt(input, lenWithUncompressedLen, key).use { plaintext ->
            val uncompressedLen = plaintext.readInt()
            check(uncompressedLen >= 0)

            try {
                type.createInputStream(ByteBufInputStream(plaintext, len), uncompressedLen).use { inputStream ->
                    /**
                     * We don't pass uncompressedLen to the buffer here: in some cases,
                     * an incorrect key can produce a valid header (particularly for
                     * LZMA, which has no magic number). If we're unlucky,
                     * uncompressedLen will be a huge number (e.g. 1 or 2 gigabytes),
                     * which might OOM some environments if allocated up front.
                     *
                     * However, if the key is incorrect it's likely that actually
                     * attempting to uncompress the data will quickly produce an error,
                     * long before we need to actually read 1 or 2 gigabytes of data.
                     * We therefore allow the buffer to grow dynamically.
                     */
                    plaintext.alloc().buffer().use { output ->
                        var remaining = uncompressedLen
                        while (remaining > 0) {
                            val n = output.writeBytes(inputStream, remaining)
                            if (n == -1) {
                                // uncompressed data truncated
                                return null
                            }
                            remaining -= n
                        }

                        if (inputStream.read() != -1) {
                            // uncompressed data overflow
                            return null
                        }

                        return output.retain()
                    }
                }
            } catch (ex: IOException) {
                return null
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

    public fun isEmptyLoc(buf: ByteBuf): Boolean {
        if (buf.readableBytes() < 5) {
            throw IOException("Missing header")
        }

        val typeId = buf.readUnsignedByte().toInt()
        val type = Js5CompressionType.fromOrdinal(typeId)
            ?: throw IOException("Invalid compression type: $typeId")

        val len = buf.readInt()
        if (len < 0) {
            throw IOException("Length is negative: $len")
        }

        if (type == Js5CompressionType.UNCOMPRESSED) {
            if (buf.readableBytes() < len) {
                throw IOException("Data truncated")
            }
            buf.skipBytes(len)

            // an empty loc group has a single byte
            return len == 1
        }

        val lenWithUncompressedLen = len + 4
        if (buf.readableBytes() < lenWithUncompressedLen) {
            throw IOException("Compressed data truncated")
        }
        buf.skipBytes(lenWithUncompressedLen)

        return when (type) {
            Js5CompressionType.UNCOMPRESSED -> throw AssertionError()
            Js5CompressionType.BZIP2 -> len == 33
            /*
             * A single byte gzip compresses to 21 bytes with levels 1 to 9,
             * and 24 bytes with level 0.
             */
            Js5CompressionType.GZIP -> len == 21 || len == 24
            Js5CompressionType.LZMA -> len == 11
        }
    }
}
