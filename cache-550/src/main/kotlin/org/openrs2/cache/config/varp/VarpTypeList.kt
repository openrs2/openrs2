package org.openrs2.cache.config.varp

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5ConfigGroup
import org.openrs2.cache.config.GroupConfigTypeList

@Singleton
public class VarpTypeList @Inject constructor(cache: Cache) : GroupConfigTypeList<VarpType>(
    cache,
    archive = Js5Archive.CONFIG,
    group = Js5ConfigGroup.VAR_PLAYER
) {
    override fun allocate(id: Int): VarpType {
        return VarpType(id)
    }
}
