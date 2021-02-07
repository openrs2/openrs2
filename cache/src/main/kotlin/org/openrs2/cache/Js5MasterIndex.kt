package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufUtil
import org.bouncycastle.crypto.params.RSAKeyParameters
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.crypto.Rsa
import org.openrs2.crypto.Whirlpool
import org.openrs2.crypto.rsaDecrypt
import org.openrs2.crypto.rsaEncrypt
import org.openrs2.crypto.whirlpool

public data class Js5MasterIndex(
    public var format: MasterIndexFormat,
    public val entries: MutableList<Entry> = mutableListOf()
) {
    public class Entry(
        public var version: Int,
        public var checksum: Int,
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
            if (digest != null) {
                if (other.digest == null) return false
                if (!digest.contentEquals(other.digest)) return false
            } else if (other.digest != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = version
            result = 31 * result + checksum
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
            return "Entry(version=$version, checksum=$checksum, digest=$hex)"
        }
    }

    public fun write(buf: ByteBuf, key: RSAKeyParameters? = null) {
        val start = buf.writerIndex()

        if (format >= MasterIndexFormat.WHIRLPOOL) {
            buf.writeByte(entries.size)
        }

        for (entry in entries) {
            buf.writeInt(entry.checksum)

            if (format >= MasterIndexFormat.VERSIONED) {
                buf.writeInt(entry.version)
            }

            if (format >= MasterIndexFormat.WHIRLPOOL) {
                val digest = entry.digest
                if (digest != null) {
                    buf.writeBytes(digest)
                } else {
                    buf.writeZero(Whirlpool.DIGESTBYTES)
                }
            }
        }

        if (format >= MasterIndexFormat.WHIRLPOOL) {
            val digest = buf.whirlpool(start, buf.writerIndex() - start)

            if (key != null) {
                buf.alloc().buffer(SIGNATURE_LENGTH, SIGNATURE_LENGTH).use { plaintext ->
                    plaintext.writeByte(Rsa.MAGIC)
                    plaintext.writeBytes(digest)

                    plaintext.rsaEncrypt(key).use { ciphertext ->
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
            for (archive in store.list(Js5Archive.ARCHIVESET)) {
                /*
                 * Fill in gaps with zeroes. I think this is consistent with
                 * the official implementation: the TFU client warns that
                 * entries with a zero CRC are probably invalid.
                 */
                for (i in nextArchive until archive) {
                    masterIndex.entries += Entry(0, 0, null)
                }

                val entry = store.read(Js5Archive.ARCHIVESET, archive).use { buf ->
                    val checksum = buf.crc32()
                    val digest = buf.whirlpool()

                    val version = Js5Compression.uncompress(buf).use { uncompressed ->
                        val index = Js5Index.read(uncompressed)

                        if (index.hasDigests) {
                            masterIndex.format = maxOf(masterIndex.format, MasterIndexFormat.WHIRLPOOL)
                        } else if (index.protocol >= Js5Protocol.VERSIONED) {
                            masterIndex.format = maxOf(masterIndex.format, MasterIndexFormat.VERSIONED)
                        }

                        index.version
                    }

                    // TODO(gpe): should we throw an exception if there are trailing bytes here or in the block above?
                    Entry(version, checksum, digest)
                }

                masterIndex.entries += entry
                nextArchive = archive + 1
            }

            return masterIndex
        }

        public fun read(buf: ByteBuf, format: MasterIndexFormat, key: RSAKeyParameters? = null): Js5MasterIndex {
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
                MasterIndexFormat.WHIRLPOOL -> {
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

                val digest = if (format >= MasterIndexFormat.WHIRLPOOL) {
                    val bytes = ByteArray(Whirlpool.DIGESTBYTES)
                    buf.readBytes(bytes)
                    bytes
                } else {
                    null
                }

                index.entries += Entry(version, checksum, digest)
            }

            val end = buf.readerIndex()

            if (format >= MasterIndexFormat.WHIRLPOOL) {
                val ciphertext = buf.readSlice(buf.readableBytes())
                decrypt(ciphertext, key).use { plaintext ->
                    require(plaintext.readableBytes() == SIGNATURE_LENGTH) {
                        "Invalid signature length"
                    }

                    // the client doesn't verify what I presume is the RSA magic byte
                    plaintext.skipBytes(1)

                    val expected = ByteArray(Whirlpool.DIGESTBYTES)
                    plaintext.readBytes(expected)

                    val actual = buf.whirlpool(start, end - start)
                    require(expected.contentEquals(actual)) {
                        "Invalid signature"
                    }
                }
            }

            return index
        }

        private fun decrypt(buf: ByteBuf, key: RSAKeyParameters?): ByteBuf {
            return if (key != null) {
                buf.rsaDecrypt(key)
            } else {
                buf.retain()
            }
        }
    }
}
