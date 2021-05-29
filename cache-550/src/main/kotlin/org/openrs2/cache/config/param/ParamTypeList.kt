package org.openrs2.cache.config.param

import org.openrs2.cache.Cache
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5ConfigGroup
import org.openrs2.cache.config.GroupConfigTypeList

public class ParamTypeList(cache: Cache) : GroupConfigTypeList<ParamType>(
    cache,
    archive = Js5Archive.CONFIG,
    group = Js5ConfigGroup.PARAMTYPE
) {
    override fun allocate(id: Int): ParamType {
        return ParamType(id)
    }
}
