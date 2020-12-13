package org.openrs2.cache

import it.unimi.dsi.fastutil.ints.Int2ObjectAVLTreeMap
import it.unimi.dsi.fastutil.ints.Int2ObjectMap
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap
import it.unimi.dsi.fastutil.ints.IntAVLTreeSet
import it.unimi.dsi.fastutil.ints.IntSortedSet
import it.unimi.dsi.fastutil.ints.IntSortedSets
import org.openrs2.util.krHashCode

/**
 * A specialist collection type entirely designed for use by the [Js5Index]
 * class.
 *
 * Entries may be accessed by their ID number or by the hash of their name, if
 * they are named.
 *
 * Entries are sorted by their ID. The IDs should be fairly dense starting
 * around 0, as the client stores entries in an array. Nevertheless, this
 * implementation currently supports sparse IDs efficiently as the entries are
 * actually stored in a tree map, primarily to make manipulation of the
 * collection simpler (the client's implementation has the luxury of being
 * read-only!)
 *
 * As the vast majority of [Js5Index.Group]s only have a single [Js5Index.File]
 * entry, there is a special case for single entry collections. This avoids the
 * memory overhead of the underlying collections in this special case, at the
 * expense of making the logic slightly more complicated.
 *
 * Entries without a name have a name hash of -1. This is consistent with the
 * client, which sets the name hash array to -1 before populating it. If the
 * index or group is sparse, any non-existent IDs still have a hash of -1, which
 * is passed into the client's IntHashTable class. This makes -1 unusable, so I
 * feel this choice is justified.
 *
 * In practice, I think indexes generally either have names for every file or
 * none at all, but allowing a mixture in this implementation makes mutating
 * the index simpler.
 *
 * This implementation permits multiple entries to have the same name hash. In
 * the event of a collision, the colliding entry with the lowest ID is returned
 * from methods that look up an entry by its name hash. This implementation
 * still behaves correctly if one of the colliding entries is removed: it will
 * always return the entry with the next lowest ID.
 *
 * This is an edge case that is unlikely to be hit in practice: the 550 cache
 * has no collisions, and the 876 cache only has a single collision (the hash
 * for the empty string).
 */
public abstract class NamedEntryCollection<T : NamedEntry>(
    private val entryConstructor: (NamedEntryCollection<T>, Int) -> T
) : MutableIterable<T> {
    private var singleEntry: T? = null
    private var entries: Int2ObjectAVLTreeMap<T>? = null

    // XXX(gpe): unfortunately fastutil doesn't have a multimap type
    private var nameHashTable: Int2ObjectMap<IntSortedSet>? = null

    public val size: Int
        get() {
            if (singleEntry != null) {
                return 1
            }

            val entries = entries ?: return 0
            return entries.size
        }

    public val capacity: Int
        get() {
            val entry = singleEntry
            if (entry != null) {
                return entry.id + 1
            }

            val entries = entries ?: return 0
            assert(entries.isNotEmpty())
            return entries.lastIntKey() + 1
        }

    public operator fun contains(id: Int): Boolean {
        require(id >= 0)

        val entry = singleEntry
        if (entry?.id == id) {
            return true
        }

        val entries = entries ?: return false
        return entries.containsKey(id)
    }

    public fun containsNamed(nameHash: Int): Boolean {
        require(nameHash != -1)

        val entry = singleEntry
        if (entry?.nameHash == nameHash) {
            return true
        }

        return nameHashTable?.containsKey(nameHash) ?: false
    }

    public operator fun contains(name: String): Boolean {
        return containsNamed(name.krHashCode())
    }

    public operator fun get(id: Int): T? {
        require(id >= 0)

        val entry = singleEntry
        if (entry?.id == id) {
            return entry
        }

        val entries = entries ?: return null
        return entries[id]
    }

    public fun getNamed(nameHash: Int): T? {
        require(nameHash != -1)

        val entry = singleEntry
        if (entry?.nameHash == nameHash) {
            return entry
        }

        val nameHashTable = nameHashTable ?: return null
        val ids = nameHashTable.getOrDefault(nameHash, IntSortedSets.EMPTY_SET)
        return if (ids.isNotEmpty()) {
            get(ids.firstInt())!!
        } else {
            null
        }
    }

    public operator fun get(name: String): T? {
        return getNamed(name.krHashCode())
    }

    public fun createOrGet(id: Int): T {
        var entry = get(id)
        if (entry != null) {
            return entry
        }

        entry = entryConstructor(this, id)

        val singleEntry = singleEntry
        var entries = entries
        if (singleEntry == null && entries == null) {
            this.singleEntry = entry
            return entry
        }

        if (entries == null) {
            entries = Int2ObjectAVLTreeMap<T>()

            if (singleEntry != null) {
                entries[singleEntry.id] = singleEntry

                if (singleEntry.nameHash != -1) {
                    val nameHashTable = Int2ObjectOpenHashMap<IntSortedSet>()
                    nameHashTable[singleEntry.nameHash] = IntSortedSets.singleton(singleEntry.id)
                    this.nameHashTable = nameHashTable
                }
            }

            this.singleEntry = null
            this.entries = entries
        }

        entries[id] = entry
        return entry
    }

    public fun createOrGetNamed(nameHash: Int): T {
        var entry = getNamed(nameHash)
        if (entry != null) {
            return entry
        }

        entry = createOrGet(allocateId())
        entry.nameHash = nameHash
        return entry
    }

    public fun createOrGet(name: String): T {
        return createOrGetNamed(name.krHashCode())
    }

    public fun remove(id: Int) {
        get(id)?.remove()
    }

    public fun removeNamed(nameHash: Int) {
        getNamed(nameHash)?.remove()
    }

    public fun remove(name: String) {
        removeNamed(name.krHashCode())
    }

    private fun allocateId(): Int {
        val size = size
        val capacity = capacity
        if (size == capacity) {
            return capacity
        }

        val singleEntry = singleEntry
        if (singleEntry != null) {
            assert(singleEntry.id != 0)
            return 0
        }

        val entries = entries!!
        for (id in 0 until capacity) {
            if (!entries.containsKey(id)) {
                return id
            }
        }

        throw AssertionError()
    }

    internal fun rename(id: Int, prevNameHash: Int, newNameHash: Int) {
        if (prevNameHash == newNameHash || singleEntry != null) {
            return
        }

        var nameHashTable = nameHashTable
        if (nameHashTable != null && prevNameHash != -1) {
            val set = nameHashTable.get(prevNameHash)
            assert(set != null && set.contains(id))

            if (set.size > 1) {
                set.remove(id)
            } else {
                nameHashTable.remove(prevNameHash)
            }
        }

        if (newNameHash != -1) {
            if (nameHashTable == null) {
                nameHashTable = Int2ObjectOpenHashMap()
            }

            val set = nameHashTable[newNameHash]
            when {
                set == null -> nameHashTable[newNameHash] = IntSortedSets.singleton(id)
                set.size == 1 -> {
                    val newSet = IntAVLTreeSet()
                    newSet.add(set.firstInt())
                    newSet.add(id)
                    nameHashTable[newNameHash] = newSet
                }
                else -> set.add(id)
            }

            this.nameHashTable = nameHashTable
        }
    }

    internal fun remove(entry: T) {
        if (singleEntry?.id == entry.id) {
            singleEntry = null
            return
        }

        val entries = entries
        check(entries != null)

        val removedEntry = entries.remove(entry.id)
        check(removedEntry != null)

        if (entries.size == 1) {
            val firstId = entries.firstIntKey()
            singleEntry = entries[firstId]
            this.entries = null
            nameHashTable = null
            return
        }

        rename(entry.id, entry.nameHash, -1)
    }

    override fun iterator(): MutableIterator<T> {
        val singleEntry = singleEntry
        if (singleEntry != null) {
            return object : MutableIterator<T> {
                private var pos = 0
                private var removed = false

                override fun hasNext(): Boolean {
                    return pos == 0
                }

                override fun next(): T {
                    if (pos++ != 0) {
                        throw NoSuchElementException()
                    }

                    return singleEntry
                }

                override fun remove() {
                    check(pos == 1 && !removed)
                    removed = true

                    singleEntry.remove()
                }
            }
        }

        val entries = entries
        if (entries != null) {
            return object : MutableIterator<T> {
                private val it = entries.values.iterator()
                private var last: T? = null

                override fun hasNext(): Boolean {
                    return it.hasNext()
                }

                override fun next(): T {
                    last = null

                    val entry = it.next()
                    last = entry
                    return entry
                }

                override fun remove() {
                    val last = last
                    check(last != null)
                    last.remove()
                    this.last = null
                }
            }
        }

        return object : MutableIterator<T> {
            override fun hasNext(): Boolean {
                return false
            }

            override fun next(): T {
                throw NoSuchElementException()
            }

            override fun remove() {
                throw IllegalStateException()
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NamedEntryCollection<*>

        if (singleEntry != other.singleEntry) return false
        if (entries != other.entries) return false

        return true
    }

    override fun hashCode(): Int {
        var result = singleEntry?.hashCode() ?: 0
        result = 31 * result + (entries?.hashCode() ?: 0)
        return result
    }
}
