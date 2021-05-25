package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufUtil
import org.openrs2.buffer.crc32
import org.openrs2.crypto.whirlpool

public class CacheArchive internal constructor(
    alloc: ByteBufAllocator,
    index: Js5Index,
    archive: Int,
    unpackedCache: UnpackedCache,
    private val store: Store
) : Archive(alloc, index, archive, unpackedCache) {
    override fun packedExists(group: Int): Boolean {
        return store.exists(archive, group)
    }

    override fun readPacked(group: Int): ByteBuf {
        return store.read(archive, group)
    }

    override fun writePacked(group: Int, buf: ByteBuf) {
        store.write(archive, group, buf)
    }

    override fun writePackedIndex(buf: ByteBuf) {
        store.write(Js5Archive.ARCHIVESET, archive, buf)
    }

    override fun removePacked(group: Int) {
        store.remove(archive, group)
    }

    override fun appendVersion(buf: ByteBuf, version: Int) {
        buf.writeShort(version)
    }

    override fun verifyCompressed(buf: ByteBuf, entry: Js5Index.MutableGroup) {
        val version = VersionTrailer.strip(buf)
        val truncatedVersion = entry.version and 0xFFFF
        if (version != truncatedVersion) {
            throw StoreCorruptException(
                "Archive $archive group ${entry.id} is out of date " +
                    "(expected version $truncatedVersion, actual version $version)"
            )
        }

        val checksum = buf.crc32()
        if (checksum != entry.checksum) {
            throw StoreCorruptException(
                "Archive $archive group ${entry.id} is corrupt " +
                    "(expected checksum ${entry.checksum}, actual checksum $checksum)"
            )
        }

        val length = buf.readableBytes()
        if (index.hasLengths && length != entry.length) {
            throw StoreCorruptException(
                "Archive $archive group ${entry.id} is corrupt " +
                    "(expected length ${entry.length}, actual length $length)"
            )
        }

        if (index.hasDigests) {
            val digest = buf.whirlpool()
            if (!digest.contentEquals(entry.digest!!)) {
                throw StoreCorruptException(
                    "Archive $archive group ${entry.id} is corrupt " +
                        "(expected digest ${ByteBufUtil.hexDump(entry.digest)}, " +
                        "actual digest ${ByteBufUtil.hexDump(digest)})"
                )
            }
        }
    }

    override fun verifyUncompressed(buf: ByteBuf, entry: Js5Index.MutableGroup) {
        val length = buf.readableBytes()
        if (index.hasLengths && length != entry.uncompressedLength) {
            throw StoreCorruptException(
                "Archive $archive group ${entry.id} is corrupt " +
                    "(expected uncompressed length ${entry.uncompressedLength}, " +
                    "actual length $length)"
            )
        }

        if (index.hasUncompressedChecksums) {
            val uncompressedChecksum = buf.crc32()
            if (uncompressedChecksum != entry.uncompressedChecksum) {
                throw StoreCorruptException(
                    "Archive $archive group ${entry.id} is corrupt " +
                        "(expected uncompressed checksum ${entry.uncompressedChecksum}, " +
                        "actual uncompressed checksum $uncompressedChecksum)"
                )
            }
        }
    }
}
