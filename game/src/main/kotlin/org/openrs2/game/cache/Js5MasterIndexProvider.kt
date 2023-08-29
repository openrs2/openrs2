package org.openrs2.game.cache

import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cache.Store

public class Js5MasterIndexProvider @Inject constructor(
    private val store: Store
) : Provider<Js5MasterIndex> {
    override fun get(): Js5MasterIndex {
        val masterIndex = Js5MasterIndex.create(store)
        masterIndex.format = MasterIndexFormat.VERSIONED
        return masterIndex
    }
}
