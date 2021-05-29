package org.openrs2.cache.config.inv

import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5ConfigGroup
import org.openrs2.cache.config.GroupConfigTypeList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class InvTypeList @Inject constructor(cache: Cache) : GroupConfigTypeList<InvType>(
    cache,
    archive = Js5Archive.CONFIG,
    group = Js5ConfigGroup.INVTYPE
) {
    override fun allocate(id: Int): InvType {
        return InvType(id)
    }
}
