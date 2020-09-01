package dev.openrs2.cache

import dev.openrs2.buffer.use
import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.io.FileNotFoundException
import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption.CREATE
import java.nio.file.StandardOpenOption.READ
import java.nio.file.StandardOpenOption.WRITE
import kotlin.math.max
import kotlin.math.min

/**
 * A [Store] implementation compatible with the native `main_file_cache.dat2`
 * and `main_file_cache.idx*` format used by the client.
 *
 * This class is not thread safe.
 */
public class DiskStore private constructor(
    private val root: Path,
    private val data: BufferedFileChannel,
    private val indexes: Array<BufferedFileChannel?>,
    private val alloc: ByteBufAllocator
) : Store {
    private data class IndexEntry(val size: Int, val block: Int)

    init {
        require(indexes.size == Store.MAX_ARCHIVE + 1)
    }

    private fun checkArchive(archive: Int) {
        require(archive in 0..Store.MAX_ARCHIVE)
    }

    private fun checkGroup(archive: Int, group: Int) {
        checkArchive(archive)

        // no upper bound on the range check here, as newer caches support 4 byte group IDs
        require(group >= 0)
    }

    private fun readIndexEntry(archive: Int, group: Int, tempBuf: ByteBuf): IndexEntry? {
        checkGroup(archive, group)

        val index = indexes[archive] ?: return null

        val pos = group.toLong() * INDEX_ENTRY_SIZE
        if ((pos + INDEX_ENTRY_SIZE) > index.size()) {
            return null
        }

        index.read(pos, tempBuf, INDEX_ENTRY_SIZE)

        val size = tempBuf.readUnsignedMedium()
        val block = tempBuf.readUnsignedMedium()
        return IndexEntry(size, block)
    }

    override fun exists(archive: Int): Boolean {
        checkArchive(archive)
        return indexes[archive] != null
    }

    override fun exists(archive: Int, group: Int): Boolean {
        alloc.buffer(TEMP_BUFFER_SIZE, TEMP_BUFFER_SIZE).use { tempBuf ->
            val entry = readIndexEntry(archive, group, tempBuf) ?: return false
            return entry.block != 0
        }
    }

    override fun list(): List<Int> {
        return indexes.withIndex()
            .filter { it.value != null }
            .map { it.index }
            .toList()
    }

    override fun list(archive: Int): List<Int> {
        checkArchive(archive)

        alloc.buffer(INDEX_BUFFER_SIZE, INDEX_BUFFER_SIZE).use { tempBuf ->
            val index = indexes[archive] ?: throw FileNotFoundException()

            val groups = mutableListOf<Int>()

            var remaining = min(index.size() / INDEX_ENTRY_SIZE, Int.MAX_VALUE.toLong()) * INDEX_ENTRY_SIZE
            var pos = 0L
            var group = 0
            while (remaining > 0) {
                tempBuf.clear()

                val n = min(remaining, tempBuf.writableBytes().toLong()).toInt()
                index.read(pos, tempBuf, n)
                pos += n
                remaining -= n

                while (tempBuf.isReadable) {
                    tempBuf.skipBytes(3)

                    val block = tempBuf.readUnsignedMedium()
                    if (block != 0) {
                        groups += group
                    }

                    group++
                }
            }

            return groups
        }
    }

    private fun createOrGetIndex(archive: Int): BufferedFileChannel {
        val index = indexes[archive]
        if (index != null) {
            return index
        }

        val newIndex = BufferedFileChannel(
            FileChannel.open(indexPath(root, archive), CREATE, READ, WRITE),
            INDEX_BUFFER_SIZE,
            INDEX_BUFFER_SIZE,
            alloc
        )
        indexes[archive] = newIndex
        return newIndex
    }

    override fun create(archive: Int) {
        checkArchive(archive)
        createOrGetIndex(archive)
    }

    override fun read(archive: Int, group: Int): ByteBuf {
        alloc.buffer(TEMP_BUFFER_SIZE, TEMP_BUFFER_SIZE).use { tempBuf ->
            val entry = readIndexEntry(archive, group, tempBuf) ?: throw FileNotFoundException()
            if (entry.block == 0) {
                throw FileNotFoundException()
            }

            alloc.buffer(entry.size, entry.size).use { buf ->
                val extended = group >= 65536
                val headerSize = if (extended) {
                    EXTENDED_BLOCK_HEADER_SIZE
                } else {
                    BLOCK_HEADER_SIZE
                }
                val dataSize = if (extended) {
                    EXTENDED_BLOCK_DATA_SIZE
                } else {
                    BLOCK_DATA_SIZE
                }

                var block = entry.block
                var num = 0
                do {
                    if (block == 0) {
                        throw StoreCorruptException("Group shorter than expected")
                    }

                    val pos = block.toLong() * BLOCK_SIZE
                    if (pos + headerSize > data.size()) {
                        throw StoreCorruptException("Next block is outside the data file")
                    }

                    // read header
                    tempBuf.clear()
                    data.read(pos, tempBuf, headerSize)

                    val actualGroup = if (extended) {
                        tempBuf.readInt()
                    } else {
                        tempBuf.readUnsignedShort()
                    }
                    val actualNum = tempBuf.readUnsignedShort()
                    val nextBlock = tempBuf.readUnsignedMedium()
                    val actualArchive = tempBuf.readUnsignedByte().toInt()

                    // verify header
                    when {
                        actualGroup != group -> throw StoreCorruptException("Expecting group $group, was $actualGroup")
                        actualNum != num -> throw StoreCorruptException("Expecting block number $num, was $actualNum")
                        actualArchive != archive ->
                            throw StoreCorruptException("Expecting archive $archive, was $actualArchive")
                    }

                    // read data
                    val len = min(buf.writableBytes(), dataSize)
                    data.read(pos + headerSize, buf, len)

                    // advance to next block
                    block = nextBlock
                    num++
                } while (buf.isWritable)

                if (block != 0) {
                    throw StoreCorruptException("Group longer than expected")
                }

                return buf.retain()
            }
        }
    }

    private fun allocateBlock(): Int {
        var block = (data.size() + BLOCK_SIZE - 1) / BLOCK_SIZE

        if (block == 0L) {
            // 0 is reserved to represent the absence of a file
            block = 1
        } else if (block < 0 || block > MAX_BLOCK) {
            throw StoreFullException()
        }

        return block.toInt()
    }

    override fun write(archive: Int, group: Int, buf: ByteBuf) {
        /*
         * This method is more complicated than both the client's
         * implementation and most existing third-party implementations that I
         * am aware of.
         *
         * Unlike the client, it is capable of overwriting a shorter group with
         * a longer one in a single pass by switching between overwrite and
         * non-overwrite modes when it reaches the end of the original group.
         *
         * The client performs this in two passes. It wastes space, as it
         * doesn't re-use any of the original group's blocks in the second
         * pass.
         *
         * Unlike most existing third-party implementations, this
         * implementation is capable of overwriting a corrupt group by
         * switching to non-overwrite mode immediately upon detecting
         * corruption, even if it hasn't hit the end of the original group yet.
         * This requires reading ahead by a block, making the logic more
         * complicated.
         *
         * Most existing third-party implementations throw an exception when
         * they attempt to overwrite a corrupt group. The client is capable o
         * overwriting corrupt groups, but as above it does so in two passes.
         * Again, this two pass approach wastes space.
         *
         * This class mixes the best features of all implementations at the
         * expense of additional complexity: all writes use a single pass, as
         * many blocks are re-used as possible (minimising the size of the
         * .dat2 file) and it is capable of overwriting corrupt groups.
         */

        checkGroup(archive, group)

        val newSize = buf.readableBytes()
        require(newSize <= Store.MAX_GROUP_SIZE)

        val index = createOrGetIndex(archive)

        alloc.buffer(TEMP_BUFFER_SIZE, TEMP_BUFFER_SIZE).use { tempBuf ->
            // read existing index entry, if it exists
            val indexPos = group.toLong() * INDEX_ENTRY_SIZE

            var block = if ((indexPos + INDEX_ENTRY_SIZE) <= index.size()) {
                index.read(indexPos, tempBuf, INDEX_ENTRY_SIZE)
                tempBuf.skipBytes(3)
                tempBuf.readUnsignedMedium()
            } else {
                0
            }

            // determine header/data sizes
            val extended = group >= 65536
            val headerSize = if (extended) {
                EXTENDED_BLOCK_HEADER_SIZE
            } else {
                BLOCK_HEADER_SIZE
            }
            val dataSize = if (extended) {
                EXTENDED_BLOCK_DATA_SIZE
            } else {
                BLOCK_DATA_SIZE
            }

            // check that the first block isn't outside the data file
            val firstBlockPos = block.toLong() * BLOCK_SIZE
            if (firstBlockPos + headerSize > data.size()) {
                block = 0
            }

            // check that the first block is valid
            var num = 0
            var nextBlock = 0
            if (block != 0) {
                tempBuf.clear()
                data.read(firstBlockPos, tempBuf, headerSize)

                val actualGroup = if (extended) {
                    tempBuf.readInt()
                } else {
                    tempBuf.readUnsignedShort()
                }
                val actualNum = tempBuf.readUnsignedShort()
                nextBlock = tempBuf.readUnsignedMedium()
                val actualArchive = tempBuf.readUnsignedByte().toInt()

                if (actualGroup != group || actualNum != num || actualArchive != archive) {
                    block = 0
                    nextBlock = 0
                }
            }

            // allocate a new block if necessary
            var overwrite: Boolean
            if (block == 0) {
                block = allocateBlock()
                overwrite = false
            } else {
                overwrite = true
            }

            // write new index entry
            tempBuf.clear()
            tempBuf.writeMedium(newSize)
            tempBuf.writeMedium(block)
            index.write(indexPos, tempBuf, INDEX_ENTRY_SIZE)

            do {
                val nextNum = num + 1
                var nextNextBlock = 0
                val len: Int
                val remaining = buf.readableBytes()
                if (remaining <= dataSize) {
                    // we're in the last block, so the next block is zero
                    len = remaining
                    nextBlock = 0
                } else {
                    len = dataSize

                    if (overwrite) {
                        // check that the next block isn't outside the data file
                        val nextBlockPos = nextBlock.toLong() * BLOCK_SIZE
                        if (nextBlockPos + headerSize > data.size()) {
                            nextBlock = 0
                        }

                        // check that the next block is valid
                        if (nextBlock != 0) {
                            tempBuf.clear()
                            data.read(nextBlockPos, tempBuf, headerSize)

                            val actualGroup = if (extended) {
                                tempBuf.readInt()
                            } else {
                                tempBuf.readUnsignedShort()
                            }
                            val actualNum = tempBuf.readUnsignedShort()
                            nextNextBlock = tempBuf.readUnsignedMedium()
                            val actualArchive = tempBuf.readUnsignedByte().toInt()

                            if (actualGroup != group || actualNum != nextNum || actualArchive != archive) {
                                nextBlock = 0
                                nextNextBlock = 0
                            }
                        }

                        // allocate a new block if necessary
                        if (nextBlock == 0) {
                            nextBlock = allocateBlock()
                            overwrite = false
                        }
                    } else {
                        nextBlock = block + 1
                        if (nextBlock > MAX_BLOCK) {
                            throw StoreFullException()
                        }
                    }
                }

                // write header
                val blockPos = block.toLong() * BLOCK_SIZE

                tempBuf.clear()
                if (extended) {
                    tempBuf.writeInt(group)
                } else {
                    tempBuf.writeShort(group)
                }
                tempBuf.writeShort(num)
                tempBuf.writeMedium(nextBlock)
                tempBuf.writeByte(archive)

                data.write(blockPos, tempBuf, headerSize)

                // write data
                data.write(blockPos + headerSize, buf, len)

                // advance to next block
                block = nextBlock
                nextBlock = nextNextBlock
                num = nextNum
            } while (buf.isReadable)
        }
    }

    override fun remove(archive: Int) {
        checkArchive(archive)

        val index = indexes[archive] ?: return
        index.close()

        Files.deleteIfExists(indexPath(root, archive))

        indexes[archive] = null
    }

    override fun remove(archive: Int, group: Int) {
        checkGroup(archive, group)

        val index = indexes[archive] ?: return

        val pos = group.toLong() * INDEX_ENTRY_SIZE
        if ((pos + INDEX_ENTRY_SIZE) > index.size()) {
            return
        }

        alloc.buffer(TEMP_BUFFER_SIZE, TEMP_BUFFER_SIZE).use { tempBuf ->
            tempBuf.writeZero(INDEX_ENTRY_SIZE)
            index.write(pos, tempBuf, INDEX_ENTRY_SIZE)
        }
    }

    override fun flush() {
        data.flush()

        for (index in indexes) {
            index?.flush()
        }
    }

    override fun close() {
        data.close()

        for (index in indexes) {
            index?.close()
        }
    }

    public companion object {
        private const val INDEX_ENTRY_SIZE = 6

        private const val BLOCK_HEADER_SIZE = 8
        private const val BLOCK_DATA_SIZE = 512
        private const val BLOCK_SIZE = BLOCK_HEADER_SIZE + BLOCK_DATA_SIZE

        private const val EXTENDED_BLOCK_HEADER_SIZE = 10
        private const val EXTENDED_BLOCK_DATA_SIZE = 510

        private const val MAX_BLOCK = (1 shl 24) - 1

        private val TEMP_BUFFER_SIZE = max(INDEX_ENTRY_SIZE, max(BLOCK_HEADER_SIZE, EXTENDED_BLOCK_HEADER_SIZE))
        private const val INDEX_BUFFER_SIZE = INDEX_ENTRY_SIZE * 1000
        private const val DATA_BUFFER_SIZE = BLOCK_SIZE * 10

        private fun dataPath(root: Path): Path {
            return root.resolve("main_file_cache.dat2")
        }

        private fun indexPath(root: Path, archive: Int): Path {
            return root.resolve("main_file_cache.idx$archive")
        }

        public fun open(root: Path, alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): Store {
            val data = BufferedFileChannel(
                FileChannel.open(dataPath(root), READ, WRITE),
                DATA_BUFFER_SIZE,
                DATA_BUFFER_SIZE,
                alloc
            )

            val archives = Array(Store.MAX_ARCHIVE + 1) { archive ->
                val path = indexPath(root, archive)
                if (Files.exists(path)) {
                    BufferedFileChannel(
                        FileChannel.open(path, READ, WRITE),
                        INDEX_BUFFER_SIZE,
                        INDEX_BUFFER_SIZE,
                        alloc
                    )
                } else {
                    null
                }
            }

            return DiskStore(root, data, archives, alloc)
        }

        public fun create(root: Path, alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): Store {
            Files.createDirectories(root)

            val data = BufferedFileChannel(
                FileChannel.open(dataPath(root), CREATE, READ, WRITE),
                DATA_BUFFER_SIZE,
                DATA_BUFFER_SIZE,
                alloc
            )

            val archives = Array<BufferedFileChannel?>(Store.MAX_ARCHIVE + 1) { null }

            return DiskStore(root, data, archives, alloc)
        }
    }
}
