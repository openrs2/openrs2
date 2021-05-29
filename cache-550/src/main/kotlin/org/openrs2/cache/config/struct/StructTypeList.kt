package org.openrs2.cache.config.struct

import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5ConfigGroup
import org.openrs2.cache.config.GroupConfigTypeList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class StructTypeList @Inject constructor(cache: Cache) : GroupConfigTypeList<StructType>(
    cache,
    archive = Js5Archive.CONFIG,
    group = Js5ConfigGroup.STRUCTTYPE
) {
    override fun allocate(id: Int): StructType {
        return StructType(id)
    }
}
