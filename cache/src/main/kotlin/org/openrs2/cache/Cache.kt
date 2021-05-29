package org.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import org.openrs2.buffer.use
import org.openrs2.crypto.XteaKey
import org.openrs2.util.krHashCode
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.Flushable
import java.nio.file.Path

/**
 * A high-level interface for reading and writing files to and from a
 * collection of JS5 archives.
 */
public class Cache private constructor(
    private val store: Store,
    private val alloc: ByteBufAllocator,
    unpackedCacheSize: Int
) : Flushable, Closeable {
    private val archives = arrayOfNulls<CacheArchive>(MAX_ARCHIVE + 1)
    private val unpackedCache = UnpackedCache(unpackedCacheSize)

    private fun init() {
        for (archive in store.list(Store.ARCHIVESET)) {
            val index = store.read(Store.ARCHIVESET, archive).use { compressed ->
                Js5Compression.uncompress(compressed).use { buf ->
                    Js5Index.read(buf)
                }
            }

            archives[archive] = CacheArchive(alloc, index, archive, unpackedCache, store)
        }
    }

    private fun createOrGetArchive(id: Int): Archive {
        var archive = archives[id]
        if (archive != null) {
            return archive
        }

        // TODO(gpe): protocol/flags should be configurable somehow
        val index = Js5Index(Js5Protocol.VERSIONED)
        archive = CacheArchive(alloc, index, id, unpackedCache, store)
        archives[id] = archive
        return archive
    }

    // TODO(gpe): rename/move, reindex, rekey, method to go from name->id

    public fun create(archive: Int) {
        checkArchive(archive)
        createOrGetArchive(archive)
    }

    public fun capacity(archive: Int): Int {
        checkArchive(archive)
        return archives[archive]?.capacity ?: throw FileNotFoundException()
    }

    public fun capacity(archive: Int, group: Int): Int {
        checkArchive(archive)
        return archives[archive]?.capacity(group) ?: throw FileNotFoundException()
    }

    public fun capacityNamed(archive: Int, groupNameHash: Int): Int {
        checkArchive(archive)
        return archives[archive]?.capacityNamed(groupNameHash) ?: throw FileNotFoundException()
    }

    public fun capacity(archive: Int, group: String): Int {
        return capacityNamed(archive, group.krHashCode())
    }

    public fun exists(archive: Int): Boolean {
        checkArchive(archive)
        return archives[archive] != null
    }

    public fun exists(archive: Int, group: Int): Boolean {
        checkArchive(archive)
        return archives[archive]?.exists(group) ?: false
    }

    public fun existsNamed(archive: Int, groupNameHash: Int): Boolean {
        checkArchive(archive)
        return archives[archive]?.existsNamed(groupNameHash) ?: false
    }

    public fun exists(archive: Int, group: String): Boolean {
        return existsNamed(archive, group.krHashCode())
    }

    public fun exists(archive: Int, group: Int, file: Int): Boolean {
        checkArchive(archive)
        return archives[archive]?.exists(group, file) ?: false
    }

    public fun existsNamed(archive: Int, groupNameHash: Int, fileNameHash: Int): Boolean {
        checkArchive(archive)
        return archives[archive]?.existsNamed(groupNameHash, fileNameHash) ?: false
    }

    public fun exists(archive: Int, group: String, file: String): Boolean {
        return existsNamed(archive, group.krHashCode(), file.krHashCode())
    }

    public fun list(): Iterator<Int> {
        return archives.withIndex()
            .filter { it.value != null }
            .map { it.index }
            .iterator()
    }

    public fun list(archive: Int): Iterator<Js5Index.Group<*>> {
        checkArchive(archive)
        return archives[archive]?.list() ?: throw FileNotFoundException()
    }

    public fun list(archive: Int, group: Int): Iterator<Js5Index.File> {
        checkArchive(archive)
        return archives[archive]?.list(group) ?: throw FileNotFoundException()
    }

    public fun listNamed(archive: Int, groupNameHash: Int): Iterator<Js5Index.File> {
        checkArchive(archive)
        return archives[archive]?.listNamed(groupNameHash) ?: throw FileNotFoundException()
    }

    public fun list(archive: Int, group: String): Iterator<Js5Index.File> {
        return listNamed(archive, group.krHashCode())
    }

    public fun read(archive: Int, group: Int, file: Int, key: XteaKey = XteaKey.ZERO): ByteBuf {
        checkArchive(archive)
        return archives[archive]?.read(group, file, key) ?: throw FileNotFoundException()
    }

    public fun readNamed(archive: Int, groupNameHash: Int, fileNameHash: Int, key: XteaKey = XteaKey.ZERO): ByteBuf {
        checkArchive(archive)
        return archives[archive]?.readNamed(groupNameHash, fileNameHash, key) ?: throw FileNotFoundException()
    }

    public fun read(archive: Int, group: String, file: String, key: XteaKey = XteaKey.ZERO): ByteBuf {
        return readNamed(archive, group.krHashCode(), file.krHashCode(), key)
    }

    public fun write(archive: Int, group: Int, file: Int, buf: ByteBuf, key: XteaKey = XteaKey.ZERO) {
        checkArchive(archive)
        createOrGetArchive(archive).write(group, file, buf, key)
    }

    public fun writeNamed(
        archive: Int,
        groupNameHash: Int,
        fileNameHash: Int,
        buf: ByteBuf,
        key: XteaKey = XteaKey.ZERO
    ) {
        checkArchive(archive)
        createOrGetArchive(archive).writeNamed(groupNameHash, fileNameHash, buf, key)
    }

    public fun write(archive: Int, group: String, file: String, buf: ByteBuf, key: XteaKey = XteaKey.ZERO) {
        writeNamed(archive, group.krHashCode(), file.krHashCode(), buf, key)
    }

    public fun remove(archive: Int) {
        checkArchive(archive)

        if (archives[archive] == null) {
            return
        }

        archives[archive] = null

        unpackedCache.remove(archive)

        store.remove(archive)
        store.remove(Store.ARCHIVESET, archive)
    }

    public fun remove(archive: Int, group: Int) {
        checkArchive(archive)
        archives[archive]?.remove(group)
    }

    public fun removeNamed(archive: Int, groupNameHash: Int) {
        checkArchive(archive)
        archives[archive]?.removeNamed(groupNameHash)
    }

    public fun remove(archive: Int, group: String) {
        return removeNamed(archive, group.krHashCode())
    }

    public fun remove(archive: Int, group: Int, file: Int, key: XteaKey = XteaKey.ZERO) {
        checkArchive(archive)
        archives[archive]?.remove(group, file, key)
    }

    public fun removeNamed(archive: Int, groupNameHash: Int, fileNameHash: Int, key: XteaKey = XteaKey.ZERO) {
        checkArchive(archive)
        archives[archive]?.removeNamed(groupNameHash, fileNameHash, key)
    }

    public fun remove(archive: Int, group: String, file: String, key: XteaKey = XteaKey.ZERO) {
        return removeNamed(archive, group.krHashCode(), file.krHashCode(), key)
    }

    /**
     * Writes pending changes back to the underlying [Store].
     */
    override fun flush() {
        unpackedCache.flush()

        for (archive in archives) {
            archive?.flush()
        }
    }

    /**
     * Writes pending changes back to the underlying [Store] and clears the
     * internal group cache.
     */
    public fun clear() {
        unpackedCache.clear()

        for (archive in archives) {
            archive?.flush()
        }
    }

    override fun close() {
        clear()
        store.close()
    }

    public companion object {
        public const val MAX_ARCHIVE: Int = 254

        public fun open(
            root: Path,
            alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
            unpackedCacheSize: Int = UnpackedCache.DEFAULT_CAPACITY
        ): Cache {
            return open(Store.open(root, alloc), alloc, unpackedCacheSize)
        }

        public fun open(
            store: Store,
            alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT,
            unpackedCacheSize: Int = UnpackedCache.DEFAULT_CAPACITY
        ): Cache {
            val cache = Cache(store, alloc, unpackedCacheSize)
            cache.init()
            return cache
        }

        private fun checkArchive(archive: Int) {
            require(archive in 0..MAX_ARCHIVE)
        }
    }
}
