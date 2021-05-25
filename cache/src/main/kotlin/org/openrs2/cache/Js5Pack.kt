package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap
import org.openrs2.buffer.use
import java.io.Closeable
import java.io.DataInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path

/**
 * A high-level interface for reading and writing files to and from a
 * single JS5 archive encoded in `.js5` format.
 */
public class Js5Pack private constructor(
    alloc: ByteBufAllocator,
    index: Js5Index,
    unpackedCacheSize: Int,
    private var packedIndex: ByteBuf,
    private val packed: Int2ObjectSortedMap<ByteBuf>,
) : Archive(alloc, index, 0, UnpackedCache(unpackedCacheSize)), Closeable {
    override fun packedExists(group: Int): Boolean {
        return packed.containsKey(group)
    }

    override fun readPacked(group: Int): ByteBuf {
        return packed[group]?.retainedSlice() ?: throw FileNotFoundException()
    }

    override fun writePacked(group: Int, buf: ByteBuf) {
        packed.put(group, buf.retain().asReadOnly())?.release()
    }

    override fun writePackedIndex(buf: ByteBuf) {
        packedIndex.release()
        packedIndex = buf.retain().asReadOnly()
    }

    override fun removePacked(group: Int) {
        packed.remove(group)?.release()
    }

    override fun appendVersion(buf: ByteBuf, version: Int) {
        // empty
    }

    override fun verifyCompressed(buf: ByteBuf, entry: Js5Index.MutableGroup) {
        // empty
    }

    override fun verifyUncompressed(buf: ByteBuf, entry: Js5Index.MutableGroup) {
        // empty
    }

    public fun write(path: Path) {
        Files.newOutputStream(path).use { output ->
            write(output)
        }
    }

    public fun write(output: OutputStream) {
        flush()

        packedIndex.getBytes(packedIndex.readerIndex(), output, packedIndex.readableBytes())

        for (compressed in packed.values) {
            compressed.getBytes(compressed.readerIndex(), output, compressed.readableBytes())
        }
    }

    override fun flush() {
        unpackedCache.flush()
        super.flush()
    }

    public fun clear() {
        unpackedCache.clear()
        super.flush()
    }

    override fun close() {
        clear()
        packedIndex.release()
        packed.values.forEach(ByteBuf::release)
    }

    public companion object {
        public fun create(
            alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
            unpackedCacheSize: Int = UnpackedCache.DEFAULT_CAPACITY
        ): Js5Pack {
            // TODO(gpe): protocol/flags should be configurable somehow
            val index = Js5Index(Js5Protocol.VERSIONED)

            alloc.buffer().use { uncompressed ->
                index.write(uncompressed)

                Js5Compression.compressBest(uncompressed).use { compressed ->
                    return Js5Pack(alloc, index, unpackedCacheSize, compressed.retain(), Int2ObjectAVLTreeMap())
                }
            }
        }

        public fun read(
            path: Path,
            alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
            unpackedCacheSize: Int = UnpackedCache.DEFAULT_CAPACITY
        ): Js5Pack {
            return Files.newInputStream(path).use { input ->
                read(input, alloc, unpackedCacheSize)
            }
        }

        public fun read(
            input: InputStream,
            alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
            unpackedCacheSize: Int = UnpackedCache.DEFAULT_CAPACITY
        ): Js5Pack {
            val dataInput = DataInputStream(input)

            readCompressed(dataInput, alloc).use { compressed ->
                val index = Js5Compression.uncompress(compressed.slice()).use { uncompressed ->
                    Js5Index.read(uncompressed)
                }

                val packed = Int2ObjectAVLTreeMap<ByteBuf>()
                try {
                    for (group in index) {
                        packed[group.id] = readCompressed(dataInput, alloc).asReadOnly()
                    }

                    packed.values.forEach(ByteBuf::retain)
                    return Js5Pack(alloc, index, unpackedCacheSize, compressed.retain(), packed)
                } finally {
                    packed.values.forEach(ByteBuf::release)
                }
            }
        }

        private fun readCompressed(input: DataInputStream, alloc: ByteBufAllocator): ByteBuf {
            val typeId = input.readUnsignedByte()
            val type = Js5CompressionType.fromOrdinal(typeId)
                ?: throw IOException("Invalid compression type: $typeId")

            val len = input.readInt()
            if (len < 0) {
                throw IOException("Length is negative: $len")
            }

            val lenWithUncompressedLen = if (type == Js5CompressionType.UNCOMPRESSED) {
                len
            } else {
                len + 4
            }

            alloc.buffer(lenWithUncompressedLen + 5, lenWithUncompressedLen + 5).use { buf ->
                buf.writeByte(typeId)
                buf.writeInt(len)
                buf.writeBytes(input, lenWithUncompressedLen)
                return buf.retain()
            }
        }
    }
}
