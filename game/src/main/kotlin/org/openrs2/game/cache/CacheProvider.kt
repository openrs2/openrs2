package org.openrs2.game.cache

import io.netty.buffer.ByteBufAllocator
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.cache.Cache
import org.openrs2.cache.Store

public class CacheProvider @Inject constructor(
    private val store: Store,
    private val alloc: ByteBufAllocator
) : Provider<Cache> {
    override fun get(): Cache {
        return Cache.open(store, alloc)
    }
}
