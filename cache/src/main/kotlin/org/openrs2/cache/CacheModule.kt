package org.openrs2.cache

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.openrs2.buffer.BufferModule
import org.openrs2.crypto.CryptoModule

public object CacheModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
        install(CryptoModule)

        bind(Store::class.java)
            .toProvider(StoreProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(Js5MasterIndex::class.java)
            .toProvider(Js5MasterIndexProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
