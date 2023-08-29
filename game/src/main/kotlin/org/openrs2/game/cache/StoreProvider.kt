package org.openrs2.game.cache

import io.netty.buffer.ByteBufAllocator
import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.cache.Store
import java.nio.file.Path

public class StoreProvider @Inject constructor(
    private val alloc: ByteBufAllocator
) : Provider<Store> {
    override fun get(): Store {
        return Store.open(Path.of("nonfree/share/cache"), alloc)
    }
}
