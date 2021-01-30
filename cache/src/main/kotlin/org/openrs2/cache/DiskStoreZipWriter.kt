package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap
import org.openrs2.buffer.use
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.zip.Deflater
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.math.min

/**
 * A specialised [Store] implementation that writes a cache in the native
 * `main_file_cache.dat2`/`main_file_cache.idx*` format to a [ZipOutputStream].
 *
 * The cache is not buffered to disk, though the index entries must be buffered
 * in memory. The memory usage is therefore proportional to the number of
 * groups, but crucially, not the size of the data file.
 *
 * This implementation only supports the [create] and [write] methods. All
 * other methods throw [UnsupportedOperationException].
 *
 * It is only intended for use by the cache archiving service's web interface.
 */
public class DiskStoreZipWriter(
    private val out: ZipOutputStream,
    private val prefix: String = "cache/",
    level: Int = Deflater.BEST_COMPRESSION,
    timestamp: Instant = Instant.EPOCH,
    private val alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT
) : Store {
    private data class IndexEntry(val len: Int, val block: Int)

    private val timestamp = FileTime.from(timestamp)
    private val indexes = arrayOfNulls<Int2ObjectSortedMap<IndexEntry>>(Store.MAX_ARCHIVE + 1)
    private val zeroBlock = ByteArray(DiskStore.BLOCK_SIZE)
    private var block = 0
    private var padding = 0

    init {
        out.setLevel(level)
        out.putNextEntry(createZipEntry("main_file_cache.dat2"))
        out.write(zeroBlock)
    }

    private fun allocateBlock(): Int {
        if (++block > DiskStore.MAX_BLOCK) {
            throw StoreFullException()
        }
        return block
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

        if (indexes[archive] == null) {
            indexes[archive] = Int2ObjectAVLTreeMap()
        }
    }

    override fun read(archive: Int, group: Int): ByteBuf {
        throw UnsupportedOperationException()
    }

    override fun write(archive: Int, group: Int, buf: ByteBuf) {
        require(archive in 0..Store.MAX_ARCHIVE)
        require(group >= 0)
        require(buf.readableBytes() <= Store.MAX_GROUP_SIZE)

        val entry = IndexEntry(buf.readableBytes(), allocateBlock())

        var index = indexes[archive]
        if (index == null) {
            index = Int2ObjectAVLTreeMap()
            indexes[archive] = index
        }
        index[group] = entry

        val extended = group >= 65536
        val dataSize = if (extended) {
            DiskStore.EXTENDED_BLOCK_DATA_SIZE
        } else {
            DiskStore.BLOCK_DATA_SIZE
        }

        var num = 0

        alloc.buffer(DiskStore.EXTENDED_BLOCK_HEADER_SIZE, DiskStore.EXTENDED_BLOCK_HEADER_SIZE).use { tempBuf ->
            do {
                // add padding to ensure we're at a multiple of the block size
                out.write(zeroBlock, 0, padding)

                // write header
                tempBuf.clear()

                if (extended) {
                    tempBuf.writeInt(group)
                } else {
                    tempBuf.writeShort(group)
                }
                tempBuf.writeShort(num++)

                if (buf.readableBytes() > dataSize) {
                    tempBuf.writeMedium(allocateBlock())
                } else {
                    tempBuf.writeMedium(0)
                }

                tempBuf.writeByte(archive)
                tempBuf.readBytes(out, tempBuf.readableBytes())

                // write data
                val len = min(buf.readableBytes(), dataSize)
                buf.readBytes(out, len)

                // remember how much padding we need to insert before the next block
                padding = dataSize - len
            } while (buf.isReadable)
        }
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

    private fun finish() {
        alloc.buffer(DiskStore.INDEX_ENTRY_SIZE, DiskStore.INDEX_ENTRY_SIZE).use { tempBuf ->
            for ((archive, index) in indexes.withIndex()) {
                if (index == null) {
                    continue
                }

                out.putNextEntry(createZipEntry("main_file_cache.idx$archive"))

                if (index.isEmpty()) {
                    continue
                }

                for (group in 0..index.lastIntKey()) {
                    val entry = index[group]
                    if (entry != null) {
                        tempBuf.clear()
                        tempBuf.writeMedium(entry.len)
                        tempBuf.writeMedium(entry.block)
                        tempBuf.readBytes(out, tempBuf.readableBytes())
                    } else {
                        out.write(zeroBlock, 0, DiskStore.INDEX_ENTRY_SIZE)
                    }
                }
            }
        }
    }

    public override fun close() {
        finish()
        out.close()
    }
}
