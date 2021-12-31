package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.crypto.Rsa
import org.openrs2.crypto.Whirlpool
import org.openrs2.crypto.rsa
import org.openrs2.crypto.whirlpool

public data class Js5MasterIndex(
    public var format: MasterIndexFormat,
    public val entries: MutableList<Entry> = mutableListOf()
) {
    public class Entry(
        public var version: Int,
        public var checksum: Int,
        public var groups: Int,
        public var totalUncompressedLength: Int,
        digest: ByteArray?
    ) {
        public var digest: ByteArray? = digest
            set(value) {
                require(value == null || value.size == Whirlpool.DIGESTBYTES)
                field = value
            }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Entry

            if (version != other.version) return false
            if (checksum != other.checksum) return false
            if (groups != other.groups) return false
            if (totalUncompressedLength != other.totalUncompressedLength) return false
            if (digest != null) {
                if (other.digest == null) return false
                if (!digest.contentEquals(other.digest)) return false
            } else if (other.digest != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + checksum
            result = 31 * result + groups
            result = 31 * result + totalUncompressedLength
            result = 31 * result + (digest?.contentHashCode() ?: 0)
            return result
        }

        override fun toString(): String {
            val digest = digest
            val hex = if (digest != null) {
                ByteBufUtil.hexDump(digest)
            } else {
                "null"
            }
            return "Entry(version=$version, checksum=$checksum, groups=$groups, " +
                "totalUncompressedLength=$totalUncompressedLength, digest=$hex)"
        }
    }

    public fun write(buf: ByteBuf, key: RSAKeyParameters? = null) {
        val start = buf.writerIndex()

        if (format >= MasterIndexFormat.DIGESTS) {
            buf.writeByte(entries.size)
        }

        for (entry in entries) {
            buf.writeInt(entry.checksum)

            if (format >= MasterIndexFormat.VERSIONED) {
                buf.writeInt(entry.version)
            }

            if (format >= MasterIndexFormat.LENGTHS) {
                buf.writeInt(entry.groups)
                buf.writeInt(entry.totalUncompressedLength)
            }

            if (format >= MasterIndexFormat.DIGESTS) {
                val digest = entry.digest
                if (digest != null) {
                    buf.writeBytes(digest)
                } else {
                    buf.writeZero(Whirlpool.DIGESTBYTES)
                }
            }
        }

        if (format >= MasterIndexFormat.DIGESTS) {
            val digest = buf.whirlpool(start, buf.writerIndex() - start)

            if (key != null) {
                buf.alloc().buffer(SIGNATURE_LENGTH, SIGNATURE_LENGTH).use { plaintext ->
                    plaintext.writeByte(Rsa.MAGIC)
                    plaintext.writeBytes(digest)

                    plaintext.rsa(key).use { ciphertext ->
                        buf.writeBytes(ciphertext)
                    }
                }
            } else {
                buf.writeByte(Rsa.MAGIC)
                buf.writeBytes(digest)
            }
        }
    }

    public companion object {
        private const val SIGNATURE_LENGTH = Whirlpool.DIGESTBYTES + 1

        public fun create(store: Store): Js5MasterIndex {
            val masterIndex = Js5MasterIndex(MasterIndexFormat.ORIGINAL)

            var nextArchive = 0
            for (archive in store.list(Store.ARCHIVESET)) {
                val entry = try {
                    store.read(Store.ARCHIVESET, archive).use { buf ->
                        val checksum = buf.crc32()
                        val digest = buf.whirlpool()

                        Js5Compression.uncompress(buf).use { uncompressed ->
                            val index = Js5Index.read(uncompressed)

                            if (index.hasLengths) {
                                masterIndex.format = maxOf(masterIndex.format, MasterIndexFormat.LENGTHS)
                            } else if (index.hasDigests) {
                                masterIndex.format = maxOf(masterIndex.format, MasterIndexFormat.DIGESTS)
                            } else if (index.protocol >= Js5Protocol.VERSIONED) {
                                masterIndex.format = maxOf(masterIndex.format, MasterIndexFormat.VERSIONED)
                            }

                            val version = index.version
                            val groups = index.size
                            val totalUncompressedLength = index.sumOf(Js5Index.Group<*>::uncompressedLength)

                            // TODO(gpe): should we throw an exception if there are trailing bytes here or in the block above?
                            Entry(version, checksum, groups, totalUncompressedLength, digest)
                        }
                    }
                } catch (ex: StoreCorruptException) {
                    /**
                     * Unused indexes are never removed from the .idx255 file
                     * by the client. If the .dat2 file reaches its maximum
                     * size, it is truncated and all block numbers in the
                     * .idx255 file will be invalid.
                     *
                     * Any in-use indexes will be overwritten, but unused
                     * indexes will remain in the .idx255 file with invalid
                     * block numbers.
                     *
                     * We therefore expect to see corrupt indexes sometimes. We
                     * ignore these as if they didn't exist.
                     */
                    continue
                }

                /*
                 * Fill in gaps with zeroes. I think this is consistent with
                 * the official implementation: the TFU client warns that
                 * entries with a zero CRC are probably invalid.
                 */
                for (i in nextArchive until archive) {
                    masterIndex.entries += Entry(0, 0, 0, 0, null)
                }

                masterIndex.entries += entry
                nextArchive = archive + 1
            }

            return masterIndex
        }

        public fun read(buf: ByteBuf, format: MasterIndexFormat, key: RSAKeyParameters? = null): Js5MasterIndex {
            return read(buf, format, key, true)
        }

        public fun readUnverified(buf: ByteBuf, format: MasterIndexFormat): Js5MasterIndex {
            return read(buf, format, null, false)
        }

        private fun read(
            buf: ByteBuf,
            format: MasterIndexFormat,
            key: RSAKeyParameters?,
            verify: Boolean
        ): Js5MasterIndex {
            val index = Js5MasterIndex(format)

            val start = buf.readerIndex()
            val len = buf.readableBytes()

            val archives = when (format) {
                MasterIndexFormat.ORIGINAL -> {
                    require(len % 4 == 0) {
                        "Length is not a multiple of 4 bytes"
                    }
                    len / 4
                }
                MasterIndexFormat.VERSIONED -> {
                    require(len % 8 == 0) {
                        "Length is not a multiple of 8 bytes"
                    }
                    len / 8
                }
                else -> {
                    buf.readUnsignedByte().toInt()
                }
            }

            for (i in 0 until archives) {
                val checksum = buf.readInt()

                val version = if (format >= MasterIndexFormat.VERSIONED) {
                    buf.readInt()
                } else {
                    0
                }

                val groups: Int
                val totalUncompressedLength: Int
                if (format >= MasterIndexFormat.LENGTHS) {
                    groups = buf.readInt()
                    totalUncompressedLength = buf.readInt()
                } else {
                    groups = 0
                    totalUncompressedLength = 0
                }

                val digest = if (format >= MasterIndexFormat.DIGESTS) {
                    val bytes = ByteArray(Whirlpool.DIGESTBYTES)
                    buf.readBytes(bytes)
                    bytes
                } else {
                    null
                }

                index.entries += Entry(version, checksum, groups, totalUncompressedLength, digest)
            }

            if (verify) {
                val end = buf.readerIndex()

                if (format >= MasterIndexFormat.DIGESTS) {
                    val ciphertext = buf.readSlice(buf.readableBytes())
                    decrypt(ciphertext, key).use { plaintext ->
                        require(plaintext.readableBytes() == SIGNATURE_LENGTH) {
                            "Invalid signature length"
                        }

                        /*
                         * This is the RSA magic byte. The client doesn't
                         * verify this (so we also skip it for compatibility),
                         * but the server does still set it to 10.
                         */
                        plaintext.skipBytes(1)

                        val expected = ByteArray(Whirlpool.DIGESTBYTES)
                        plaintext.readBytes(expected)

                        val actual = buf.whirlpool(start, end - start)
                        require(expected.contentEquals(actual)) {
                            "Invalid signature"
                        }
                    }
                }
            }

            return index
        }

        private fun decrypt(buf: ByteBuf, key: RSAKeyParameters?): ByteBuf {
            return if (key != null) {
                buf.rsa(key)
            } else {
                buf.retain()
            }
        }
    }
}
