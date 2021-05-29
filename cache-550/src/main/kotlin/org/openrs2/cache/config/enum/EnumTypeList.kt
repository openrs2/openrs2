package org.openrs2.cache.config.enum

import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.config.ArchiveConfigTypeList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class EnumTypeList @Inject constructor(cache: Cache) : ArchiveConfigTypeList<EnumType>(
    cache,
    archive = Js5Archive.CONFIG_ENUM,
    fileBits = 8
) {
    override fun allocate(id: Int): EnumType {
        return EnumType(id)
    }
}
