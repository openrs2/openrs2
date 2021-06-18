package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMap
import it.unimi.dsi.fastutil.ints.Int2ObjectSortedMaps
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.crypto.XteaKey
import org.openrs2.crypto.whirlpool
import org.openrs2.util.krHashCode
import java.io.FileNotFoundException
import java.io.Flushable

public abstract class Archive internal constructor(
    protected val alloc: ByteBufAllocator,
    protected val index: Js5Index,
    protected val archive: Int,
    internal val unpackedCache: UnpackedCache
) : Flushable {
    private var dirty = false

    internal inner class Unpacked(
        private val entry: Js5Index.MutableGroup,
        val key: XteaKey,
        private var files: Int2ObjectSortedMap<ByteBuf>
    ) {
        private var dirty = false

        private fun ensureWritable() {
            if (files.size == 1 && files is Int2ObjectSortedMaps.Singleton) {
                files = Int2ObjectAVLTreeMap(files)
            }
        }

        fun read(file: Int): ByteBuf {
            val fileEntry = entry[file] ?: throw FileNotFoundException()
            return files[fileEntry.id]!!.retainedSlice()
        }

        fun readNamed(fileNameHash: Int): ByteBuf {
            val fileEntry = entry.getNamed(fileNameHash) ?: throw FileNotFoundException()
            return files[fileEntry.id]!!.retainedSlice()
        }

        fun write(file: Int, buf: ByteBuf) {
            ensureWritable()

            val fileEntry = entry.createOrGet(file)
            files.put(fileEntry.id, buf.copy().asReadOnly())?.release()
            dirty = true
        }

        fun writeNamed(fileNameHash: Int, buf: ByteBuf) {
            ensureWritable()

            val fileEntry = entry.createOrGetNamed(fileNameHash)
            files.put(fileEntry.id, buf.copy().asReadOnly())?.release()
            dirty = true
        }

        fun remove(file: Int) {
            ensureWritable()

            val fileEntry = entry.remove(file) ?: return
            files.remove(fileEntry.id)?.release()
            dirty = true
        }

        fun removeNamed(fileNameHash: Int) {
            ensureWritable()

            val fileEntry = entry.removeNamed(fileNameHash) ?: return
            files.remove(fileEntry.id)?.release()
            dirty = true
        }

        fun flush() {
            if (!dirty) {
                return
            }

            Group.pack(files).use { buf ->
                if (index.hasLengths) {
                    entry.uncompressedLength = buf.readableBytes()
                }

                if (index.hasUncompressedChecksums) {
                    entry.uncompressedChecksum = buf.crc32()
                }

                Js5Compression.compressBest(buf, key = key).use { compressed ->
                    entry.checksum = compressed.crc32()

                    if (index.hasLengths) {
                        entry.length = compressed.readableBytes()
                    }

                    if (index.hasDigests) {
                        entry.digest = compressed.whirlpool()
                    }

                    appendVersion(compressed, ++entry.version)
                    writePacked(entry.id, compressed)
                }
            }

            dirty = false
        }

        fun release() {
            files.values.forEach(ByteBuf::release)
        }
    }

    // TODO(gpe): rename/move, reindex, rekey, method to go from name->id

    public val capacity: Int = index.capacity

    public fun capacity(group: Int): Int {
        val entry = index[group] ?: throw FileNotFoundException()
        return entry.capacity
    }

    public fun capacityNamed(groupNameHash: Int): Int {
        val entry = index.getNamed(groupNameHash) ?: throw FileNotFoundException()
        return entry.capacity
    }

    public fun capacity(group: String): Int {
        return capacityNamed(group.krHashCode())
    }

    public fun exists(group: Int): Boolean {
        require(group >= 0)
        return index.contains(group)
    }

    public fun existsNamed(groupNameHash: Int): Boolean {
        return index.containsNamed(groupNameHash)
    }

    public fun exists(group: String): Boolean {
        return existsNamed(group.krHashCode())
    }

    public fun exists(group: Int, file: Int): Boolean {
        require(group >= 0 && file >= 0)

        val entry = index[group] ?: return false
        return entry.contains(file)
    }

    public fun existsNamed(groupNameHash: Int, fileNameHash: Int): Boolean {
        val entry = index.getNamed(groupNameHash) ?: return false
        return entry.containsNamed(fileNameHash)
    }

    public fun exists(group: String, file: String): Boolean {
        return existsNamed(group.krHashCode(), file.krHashCode())
    }

    public fun list(): Iterator<Js5Index.Group<*>> {
        return index.iterator()
    }

    public fun list(group: Int): Iterator<Js5Index.File> {
        require(group >= 0)

        val entry = index[group] ?: throw FileNotFoundException()
        return entry.iterator()
    }

    public fun listNamed(groupNameHash: Int): Iterator<Js5Index.File> {
        val entry = index.getNamed(groupNameHash) ?: throw FileNotFoundException()
        return entry.iterator()
    }

    public fun list(group: String): Iterator<Js5Index.File> {
        return listNamed(group.krHashCode())
    }

    public fun read(group: Int, file: Int, key: XteaKey = XteaKey.ZERO): ByteBuf {
        require(group >= 0 && file >= 0)

        val entry = index[group] ?: throw FileNotFoundException()
        val unpacked = getUnpacked(entry, key)
        return unpacked.read(file)
    }

    public fun readNamed(groupNameHash: Int, fileNameHash: Int, key: XteaKey = XteaKey.ZERO): ByteBuf {
        val entry = index.getNamed(groupNameHash) ?: throw FileNotFoundException()
        val unpacked = getUnpacked(entry, key)
        return unpacked.readNamed(fileNameHash)
    }

    public fun read(group: String, file: String, key: XteaKey = XteaKey.ZERO): ByteBuf {
        return readNamed(group.krHashCode(), file.krHashCode(), key)
    }

    public fun write(group: Int, file: Int, buf: ByteBuf, key: XteaKey = XteaKey.ZERO) {
        require(group >= 0 && file >= 0)

        val entry = index.createOrGet(group)
        val unpacked = createOrGetUnpacked(entry, key, isOverwriting(entry, file))
        unpacked.write(file, buf)

        dirty = true
    }

    public fun writeNamed(groupNameHash: Int, fileNameHash: Int, buf: ByteBuf, key: XteaKey = XteaKey.ZERO) {
        val entry = index.createOrGetNamed(groupNameHash)
        val unpacked = createOrGetUnpacked(entry, key, isOverwritingNamed(entry, fileNameHash))
        unpacked.writeNamed(fileNameHash, buf)

        dirty = true
        index.hasNames = true
    }

    public fun write(group: String, file: String, buf: ByteBuf, key: XteaKey = XteaKey.ZERO) {
        return writeNamed(group.krHashCode(), file.krHashCode(), buf, key)
    }

    public fun remove(group: Int) {
        require(group >= 0)

        val entry = index.remove(group) ?: return
        unpackedCache.remove(archive, entry.id)
        removePacked(entry.id)

        dirty = true
    }

    public fun removeNamed(groupNameHash: Int) {
        val entry = index.removeNamed(groupNameHash) ?: return
        unpackedCache.remove(archive, entry.id)
        removePacked(entry.id)

        dirty = true
    }

    public fun remove(group: String) {
        return removeNamed(group.krHashCode())
    }

    public fun remove(group: Int, file: Int, key: XteaKey = XteaKey.ZERO) {
        require(group >= 0 && file >= 0)

        val entry = index[group] ?: return

        if (isOverwriting(entry, file)) {
            remove(group)
            return
        }

        val unpacked = getUnpacked(entry, key)
        unpacked.remove(file)

        dirty = true
    }

    public fun removeNamed(groupNameHash: Int, fileNameHash: Int, key: XteaKey = XteaKey.ZERO) {
        val entry = index.getNamed(groupNameHash) ?: return

        if (isOverwritingNamed(entry, fileNameHash)) {
            removeNamed(groupNameHash)
            return
        }

        val unpacked = getUnpacked(entry, key)
        unpacked.removeNamed(fileNameHash)

        dirty = true
    }

    public fun remove(group: String, file: String, key: XteaKey = XteaKey.ZERO) {
        return removeNamed(group.krHashCode(), file.krHashCode(), key)
    }

    public override fun flush() {
        if (!dirty) {
            return
        }

        index.version++

        alloc.buffer().use { buf ->
            index.write(buf)

            Js5Compression.compressBest(buf).use { compressed ->
                writePackedIndex(compressed)
            }
        }

        dirty = false
    }

    protected abstract fun packedExists(group: Int): Boolean
    protected abstract fun readPacked(group: Int): ByteBuf
    protected abstract fun writePacked(group: Int, buf: ByteBuf)
    protected abstract fun writePackedIndex(buf: ByteBuf)
    protected abstract fun removePacked(group: Int)
    protected abstract fun appendVersion(buf: ByteBuf, version: Int)
    protected abstract fun verifyCompressed(buf: ByteBuf, entry: Js5Index.MutableGroup)
    protected abstract fun verifyUncompressed(buf: ByteBuf, entry: Js5Index.MutableGroup)

    private fun isOverwriting(entry: Js5Index.MutableGroup, file: Int): Boolean {
        val fileEntry = entry.singleOrNull() ?: return false
        return fileEntry.id == file
    }

    private fun isOverwritingNamed(entry: Js5Index.MutableGroup, fileNameHash: Int): Boolean {
        val fileEntry = entry.singleOrNull() ?: return false
        return fileEntry.nameHash == fileNameHash
    }

    private fun createOrGetUnpacked(entry: Js5Index.MutableGroup, key: XteaKey, overwrite: Boolean): Unpacked {
        return if (entry.size == 0 || overwrite) {
            val unpacked = Unpacked(entry, key, Int2ObjectAVLTreeMap())
            unpackedCache.put(archive, entry.id, unpacked)
            return unpacked
        } else {
            getUnpacked(entry, key)
        }
    }

    private fun getUnpacked(entry: Js5Index.MutableGroup, key: XteaKey): Unpacked {
        var unpacked = unpackedCache.get(archive, entry.id)
        if (unpacked != null) {
            /*
             * If we've already unpacked the group, we check the programmer
             * is using the correct key to ensure the code always works,
             * regardless of group cache size/invalidation behaviour.
             */
            require(unpacked.key == key) {
                "Invalid key for archive $archive group ${entry.id} (expected ${unpacked!!.key}, actual $key)"
            }
            return unpacked
        }

        if (!packedExists(entry.id)) {
            throw StoreCorruptException("Archive $archive group ${entry.id} is missing")
        }

        val files = readPacked(entry.id).use { compressed ->
            // TODO(gpe): check for trailing data?
            verifyCompressed(compressed, entry)

            Js5Compression.uncompress(compressed, key).use { buf ->
                verifyUncompressed(buf, entry)

                Group.unpack(buf, entry)
            }
        }

        files.replaceAll { _, buf -> buf.asReadOnly() }

        unpacked = Unpacked(entry, key, files)
        unpackedCache.put(archive, entry.id, unpacked)
        return unpacked
    }
}
