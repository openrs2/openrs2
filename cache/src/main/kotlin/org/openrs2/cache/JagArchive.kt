package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import io.netty.buffer.ByteBufInputStream
import io.netty.buffer.ByteBufOutputStream
import it.unimi.dsi.fastutil.ints.Int2ObjectLinkedOpenHashMap
import org.openrs2.buffer.use
import org.openrs2.compress.bzip2.Bzip2
import org.openrs2.util.jagHashCode
import java.io.Closeable
import java.io.FileNotFoundException

/**
 * An interface for reading and writing `.jag` archives, which are used by
 * RuneScape Classic and early versions of RuneScape 2.
 *
 * Unlike the client, this implementation is case-sensitive. Entry names should
 * therefore be supplied in uppercase for compatibility.
 *
 * This class is not thread safe.
 */
public class JagArchive : Closeable {
    private val entries = Int2ObjectLinkedOpenHashMap<ByteBuf>()

    /**
     * The number of entries in the archive.
     */
    public val size: Int
        get() = entries.size

    /**
     * Lists all entries in the archive.
     * @return a sorted list of entry name hashes.
     */
    public fun list(): Iterator<Int> {
        return entries.keys.iterator()
    }

    /**
     * Checks whether an entry exists.
     * @param name the entry's name.
     * @return `true` if so, `false` otherwise.
     */
    public fun exists(name: String): Boolean {
        return existsNamed(name.jagHashCode())
    }

    /**
     * Checks whether an entry exists.
     * @param nameHash the entry's name hash.
     * @return `true` if so, `false` otherwise.
     */
    public fun existsNamed(nameHash: Int): Boolean {
        return entries.containsKey(nameHash)
    }

    /**
     * Reads an entry.
     *
     * This method allocates and returns a new [ByteBuf]. It is the caller's
     * responsibility to release the [ByteBuf].
     * @param name the entry's name.
     * @return the contents of the entry.
     * @throws FileNotFoundException if the entry does not exist.
     */
    public fun read(name: String): ByteBuf {
        return readNamed(name.jagHashCode())
    }

    /**
     * Reads an entry.
     *
     * This method allocates and returns a new [ByteBuf]. It is the caller's
     * responsibility to release the [ByteBuf].
     * @param nameHash the entry's name hash.
     * @return the contents of the entry.
     * @throws FileNotFoundException if the entry does not exist.
     */
    public fun readNamed(nameHash: Int): ByteBuf {
        val buf = entries[nameHash] ?: throw FileNotFoundException()
        return buf.retainedSlice()
    }

    /**
     * Writes an entry. If an entry with the same name hash already exists, it
     * is overwritten.
     * @param name the entry's name.
     * @param buf the new contents of the entry.
     */
    public fun write(name: String, buf: ByteBuf) {
        writeNamed(name.jagHashCode(), buf)
    }

    /**
     * Writes an entry. If an entry with the same name hash already exists, it
     * is overwritten.
     * @param nameHash the entry's name hash.
     * @param buf the new contents of the entry.
     */
    public fun writeNamed(nameHash: Int, buf: ByteBuf) {
        entries.put(nameHash, buf.copy().asReadOnly())?.release()
    }

    /**
     * Deletes an entry. Does nothing if the entry does not exist.
     * @param name the entry's name.
     */
    public fun remove(name: String) {
        removeNamed(name.jagHashCode())
    }

    /**
     * Deletes an entry. Does nothing if the entry does not exist.
     * @param nameHash the entry's name hash.
     */
    public fun removeNamed(nameHash: Int) {
        entries.remove(nameHash)?.release()
    }

    /**
     * Packs a `.jag` archive into a compressed [ByteBuf] using the given
     * compression method.
     *
     * This method allocates and returns a new [ByteBuf]. It is the caller's
     * responsibility to release the [ByteBuf].
     * @param compressedArchive `true` if the archive should be compressed as a
     * whole, `false` if each entry should be compressed individually.
     * @param alloc the allocator.
     * @return the compressed archive.
     */
    public fun pack(compressedArchive: Boolean, alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): ByteBuf {
        alloc.buffer().use { output ->
            alloc.buffer().use { uncompressedArchiveBuf ->
                uncompressedArchiveBuf.writeShort(size)
                var index = uncompressedArchiveBuf.writerIndex() + size * 10

                for ((nameHash, uncompressedEntryBuf) in entries) {
                    uncompressedArchiveBuf.writeInt(nameHash)
                    uncompressedArchiveBuf.writeMedium(uncompressedEntryBuf.readableBytes())

                    compress(uncompressedEntryBuf.slice(), !compressedArchive).use { entryBuf ->
                        val entryLen = entryBuf.readableBytes()
                        uncompressedArchiveBuf.writeMedium(entryLen)

                        uncompressedArchiveBuf.setBytes(index, entryBuf)
                        index += entryLen
                    }
                }

                uncompressedArchiveBuf.writerIndex(index)

                val uncompressedLen = uncompressedArchiveBuf.readableBytes()
                output.writeMedium(uncompressedLen)

                compress(uncompressedArchiveBuf, compressedArchive).use { archiveBuf ->
                    val len = archiveBuf.readableBytes()
                    if (compressedArchive && uncompressedLen == len) {
                        /*
                         * This is a bit of an odd corner case. If whole
                         * archive compression is enabled and the lengths are
                         * equal we have to use individual entry compression
                         * instead, as the lengths being equal signals to the
                         * client that this mode should be used.
                         *
                         * If anyone finds a suitable test case for this case,
                         * I'd love to see it!
                         */
                        return pack(false, alloc)
                    }

                    output.writeMedium(len)
                    output.writeBytes(archiveBuf)
                }

                return output.retain()
            }
        }
    }

    /**
     * Packs a `.jag` archive into a compressed [ByteBuf]. The best compression
     * method for minimising the size of the compressed archive is
     * automatically selected. Note that this does not necessarily correspond
     * to the minimimal amount of RAM usage at runtime.
     *
     * This method allocates and returns a new [ByteBuf]. It is the caller's
     * responsibility to release the [ByteBuf].
     * @param alloc the allocator.
     * @return the compressed archive.
     */
    public fun packBest(alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT): ByteBuf {
        pack(true, alloc).use { compressedArchive ->
            pack(false, alloc).use { compressedEntries ->
                // If equal, pick the archive that only requires one
                // decompression instead of several to save CPU time.
                return if (compressedArchive.readableBytes() <= compressedEntries.readableBytes()) {
                    compressedArchive.retain()
                } else {
                    compressedEntries.retain()
                }
            }
        }
    }

    override fun close() {
        entries.values.forEach(ByteBuf::release)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as JagArchive

        if (entries != other.entries) return false

        return true
    }

    override fun hashCode(): Int {
        return entries.hashCode()
    }

    public companion object {
        /**
         * Unpacks a [ByteBuf] containing a compressed `.jag` archive.
         * @param buf the compressed archive.
         * @return the unpacked archive.
         */
        public fun unpack(buf: ByteBuf): JagArchive {
            val archive = JagArchive()

            val uncompressedLen = buf.readUnsignedMedium()
            val len = buf.readUnsignedMedium()

            val compressedArchive = len != uncompressedLen

            uncompress(buf, compressedArchive, len, uncompressedLen).use { archiveBuf ->
                val size = archiveBuf.readUnsignedShort()
                var index = archiveBuf.readerIndex() + size * 10

                for (id in 0 until size) {
                    val nameHash = archiveBuf.readInt()
                    val uncompressedEntryLen = archiveBuf.readUnsignedMedium()
                    val entryLen = archiveBuf.readUnsignedMedium()

                    val entry = archiveBuf.slice(index, entryLen)

                    uncompress(entry, !compressedArchive, entryLen, uncompressedEntryLen).use { entryBuf ->
                        if (!archive.existsNamed(nameHash)) {
                            /*
                             * Store the first entry if there is a collision,
                             * for compatibility with the client.
                             */
                            archive.writeNamed(nameHash, entryBuf)
                        }
                    }

                    index += entryLen
                }
            }

            return archive
        }

        private fun compress(input: ByteBuf, compressed: Boolean): ByteBuf {
            return if (compressed) {
                input.alloc().buffer().use { output ->
                    Bzip2.createHeaderlessOutputStream(ByteBufOutputStream(output)).use { stream ->
                        input.readBytes(stream, input.readableBytes())
                    }

                    output.retain()
                }
            } else {
                input.readRetainedSlice(input.readableBytes())
            }
        }

        private fun uncompress(buf: ByteBuf, compressed: Boolean, compressedLen: Int, uncompressedLen: Int): ByteBuf {
            return if (compressed) {
                buf.alloc().buffer(uncompressedLen, uncompressedLen).use { output ->
                    Bzip2.createHeaderlessInputStream(ByteBufInputStream(buf.readSlice(compressedLen))).use { stream ->
                        output.writeBytes(stream, uncompressedLen)
                    }

                    output.retain()
                }
            } else {
                buf.readRetainedSlice(compressedLen)
            }
        }
    }
}
