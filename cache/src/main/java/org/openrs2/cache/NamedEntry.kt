package org.openrs2.cache

import org.openrs2.util.krHashCode

public interface NamedEntry {
    public val id: Int
    public var nameHash: Int

    public fun setName(name: String) {
        nameHash = name.krHashCode()
    }

    public fun clearName() {
        nameHash = -1
    }

    public fun remove()
}
