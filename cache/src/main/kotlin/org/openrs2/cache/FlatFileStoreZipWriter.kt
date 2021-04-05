package org.openrs2.cache

import io.netty.buffer.ByteBuf
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * A specialised [Store] implementation that writes a cache in the
 * [FlatFileStore] format to a [ZipOutputStream].
 *
 * The cache is not buffered to disk.
 *
 * This implementation only supports the [create] and [write] methods. All
 * other methods throw [UnsupportedOperationException].
 *
 * It is only intended for use by the cache archiving service's web interface.
 */
public class FlatFileStoreZipWriter(
    private val out: ZipOutputStream,
    private val prefix: String = "cache/",
    level: Int = Deflater.BEST_COMPRESSION,
    timestamp: Instant = Instant.EPOCH
) : Store {
    private val timestamp = FileTime.from(timestamp)

    init {
        out.setLevel(level)
    }

    private fun createZipEntry(name: String): ZipEntry {
        val entry = ZipEntry(prefix + name)
        entry.creationTime = timestamp
        entry.lastAccessTime = timestamp
        entry.lastModifiedTime = timestamp
        return entry
    }

    override fun exists(archive: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override fun exists(archive: Int, group: Int): Boolean {
        throw UnsupportedOperationException()
    }

    override fun list(): List<Int> {
        throw UnsupportedOperationException()
    }

    override fun list(archive: Int): List<Int> {
        throw UnsupportedOperationException()
    }

    override fun create(archive: Int) {
        require(archive in 0..Store.MAX_ARCHIVE)

        out.putNextEntry(createZipEntry("$archive/"))
    }

    override fun read(archive: Int, group: Int): ByteBuf {
        throw UnsupportedOperationException()
    }

    override fun write(archive: Int, group: Int, buf: ByteBuf) {
        require(archive in 0..Store.MAX_ARCHIVE)
        require(group >= 0)
        require(buf.readableBytes() <= Store.MAX_GROUP_SIZE)

        out.putNextEntry(createZipEntry("$archive/$group.dat"))
        buf.readBytes(out, buf.readableBytes())
    }

    override fun remove(archive: Int) {
        throw UnsupportedOperationException()
    }

    override fun remove(archive: Int, group: Int) {
        throw UnsupportedOperationException()
    }

    override fun flush() {
        out.flush()
    }

    override fun close() {
        out.close()
    }
}
