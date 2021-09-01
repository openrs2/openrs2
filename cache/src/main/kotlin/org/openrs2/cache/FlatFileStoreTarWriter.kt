package org.openrs2.cache

import io.netty.buffer.ByteBuf
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import java.time.Instant
import java.util.Date

/**
 * A specialised [Store] implementation that writes a cache in the
 * [FlatFileStore] format to a [TarArchiveOutputStream].
 *
 * The cache is not buffered to disk.
 *
 * This implementation only supports the [create] and [write] methods. All
 * other methods throw [UnsupportedOperationException].
 *
 * It is only intended for use by the cache archiving service's web interface.
 */
public class FlatFileStoreTarWriter(
    private val out: TarArchiveOutputStream,
    private val prefix: String = "cache/",
    timestamp: Instant = Instant.EPOCH
) : Store {
    private val timestamp = Date.from(timestamp)

    private fun createTarEntry(name: String, size: Int): TarArchiveEntry {
        val entry = TarArchiveEntry(prefix + name)
        entry.modTime = timestamp
        entry.size = size.toLong()
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

        out.putArchiveEntry(createTarEntry("$archive/", size = 0))
        out.closeArchiveEntry()
    }

    override fun read(archive: Int, group: Int): ByteBuf {
        throw UnsupportedOperationException()
    }

    override fun write(archive: Int, group: Int, buf: ByteBuf) {
        require(archive in 0..Store.MAX_ARCHIVE)
        require(group >= 0)
        require(buf.readableBytes() <= Store.MAX_GROUP_SIZE)

        out.putArchiveEntry(createTarEntry("$archive/$group.dat", buf.readableBytes()))
        buf.readBytes(out, buf.readableBytes())
        out.closeArchiveEntry()
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
