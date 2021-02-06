package org.openrs2.cache

import com.google.inject.AbstractModule
import org.openrs2.buffer.BufferModule
import org.openrs2.crypto.CryptoModule

public object CacheModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
        install(CryptoModule)
    }
}
