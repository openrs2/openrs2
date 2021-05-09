package org.openrs2.cache

import org.openrs2.util.krHashCode

public interface MutableNamedEntry : NamedEntry {
    public override var nameHash: Int

    public fun setName(name: String) {
        nameHash = name.krHashCode()
    }

    public fun clearName() {
        nameHash = -1
    }

    public fun remove()
}
