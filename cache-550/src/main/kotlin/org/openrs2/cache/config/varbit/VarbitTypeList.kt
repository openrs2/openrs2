package org.openrs2.cache.config.varbit

import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.config.ArchiveConfigTypeList

public class VarbitTypeList(cache: Cache) : ArchiveConfigTypeList<VarbitType>(
    cache,
    archive = Js5Archive.CONFIG_VAR_BIT,
    fileBits = 10
) {
    override fun allocate(id: Int): VarbitType {
        return VarbitType(id)
    }
}
