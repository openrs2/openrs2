package org.openrs2.cache.config

import org.openrs2.cache.Cache

public abstract class ArchiveConfigTypeList<T : ConfigType>(
    cache: Cache,
    archive: Int,
    private val fileBits: Int
) : ConfigTypeList<T>(cache, archive) {
    private val fileMask = (1 shl fileBits) - 1

    override fun calculateCapacity(): Int {
        val archiveCapacity = cache.capacity(archive)
        if (archiveCapacity == 0) {
            return 0
        }

        val groupCapacity = cache.capacity(archive, archiveCapacity - 1)
        return ((archiveCapacity - 1) shl fileBits) or groupCapacity
    }

    override fun getGroupId(id: Int): Int {
        return id ushr fileBits
    }

    override fun getFileId(id: Int): Int {
        return id and fileMask
    }
}
