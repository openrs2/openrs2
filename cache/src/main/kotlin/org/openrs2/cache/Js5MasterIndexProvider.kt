package org.openrs2.cache

import javax.inject.Inject
import javax.inject.Provider

public class Js5MasterIndexProvider @Inject constructor(
    private val store: Store
) : Provider<Js5MasterIndex> {
    override fun get(): Js5MasterIndex {
        return Js5MasterIndex.create(store)
    }
}
