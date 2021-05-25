package org.openrs2.cache

import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap
import it.unimi.dsi.fastutil.longs.LongArrayList

internal class UnpackedCache(
    private val capacity: Int
) {
    private val cache = Long2ObjectLinkedOpenHashMap<Archive.Unpacked>()

    init {
        require(capacity >= 1)
    }

    fun get(archive: Int, group: Int): Archive.Unpacked? {
        return cache.getAndMoveToLast(key(archive, group))
    }

    fun put(archive: Int, group: Int, unpacked: Archive.Unpacked) {
        while (cache.size >= capacity) {
            val lru = cache.removeFirst()
            lru.flush()
            lru.release()
        }

        cache.putAndMoveToLast(key(archive, group), unpacked)?.release()
    }

    fun remove(archive: Int) {
        val start = key(archive, 0)
        val end = key(archive + 1, 0)

        val keys = LongArrayList()

        val it = cache.keys.iterator(start)
        while (it.hasNext()) {
            val key = it.nextLong()
            if (key >= end) {
                break
            }
            keys += key
        }

        for (i in 0 until keys.size) {
            cache.remove(keys.getLong(i)).release()
        }
    }

    fun remove(archive: Int, group: Int) {
        cache.remove(key(archive, group))?.release()
    }

    fun flush() {
        for (unpacked in cache.values) {
            unpacked.flush()
        }
    }

    fun clear() {
        for (unpacked in cache.values) {
            unpacked.flush()
            unpacked.release()
        }

        cache.clear()
    }

    private fun key(archive: Int, group: Int): Long {
        return (archive.toLong() shl 32) or group.toLong()
    }

    companion object {
        const val DEFAULT_CAPACITY: Int = 1024
    }
}
