package org.openrs2.cache.config

import org.openrs2.cache.Cache

public abstract class GroupConfigTypeList<T : ConfigType>(
    cache: Cache,
    archive: Int,
    private val group: Int
) : ConfigTypeList<T>(cache, archive) {
    override fun calculateCapacity(): Int {
        return cache.capacity(archive, group)
    }

    override fun getGroupId(id: Int): Int {
        return group
    }

    override fun getFileId(id: Int): Int {
        return id
    }
}
