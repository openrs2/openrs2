package org.openrs2.cache

import org.openrs2.util.krHashCode

/**
 * A read-only view of a [MutableNamedEntryCollection].
 */
public interface NamedEntryCollection<out T : NamedEntry> : Iterable<T> {
    public val size: Int
    public val capacity: Int

    public operator fun contains(id: Int): Boolean
    public fun containsNamed(nameHash: Int): Boolean

    public operator fun contains(name: String): Boolean {
        return containsNamed(name.krHashCode())
    }

    public operator fun get(id: Int): T?
    public fun getNamed(nameHash: Int): T?

    public operator fun get(name: String): T? {
        return getNamed(name.krHashCode())
    }
}
