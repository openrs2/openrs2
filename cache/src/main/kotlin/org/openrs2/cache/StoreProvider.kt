package org.openrs2.cache

import io.netty.buffer.ByteBufAllocator
import java.nio.file.Path
import javax.inject.Inject
import javax.inject.Provider

public class StoreProvider @Inject constructor(
    private val alloc: ByteBufAllocator
) : Provider<Store> {
    override fun get(): Store {
        return Store.open(Path.of("nonfree/share/cache"), alloc)
    }
}
