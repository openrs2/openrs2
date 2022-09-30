package org.openrs2.game

import com.google.common.util.concurrent.Service
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.openrs2.buffer.BufferModule
import org.openrs2.cache.Cache
import org.openrs2.cache.CacheModule
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.Store
import org.openrs2.conf.ConfigModule
import org.openrs2.game.cache.CacheProvider
import org.openrs2.game.cache.Js5MasterIndexProvider
import org.openrs2.game.cache.StoreProvider
import org.openrs2.game.cluster.Cluster
import org.openrs2.game.cluster.SingleWorldCluster
import org.openrs2.game.net.NetworkService
import org.openrs2.game.net.js5.Js5Service
import org.openrs2.game.store.DummyPlayerStore
import org.openrs2.game.store.PlayerStore
import org.openrs2.net.NetworkModule
import org.openrs2.protocol.ProtocolModule

public object GameModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
        install(CacheModule)
        install(ConfigModule)
        install(NetworkModule)
        install(ProtocolModule)

        val binder = Multibinder.newSetBinder(binder(), Service::class.java)
        binder.addBinding().to(GameService::class.java)
        binder.addBinding().to(Js5Service::class.java)
        binder.addBinding().to(NetworkService::class.java)

        bind(Store::class.java)
            .toProvider(StoreProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(Js5MasterIndex::class.java)
            .toProvider(Js5MasterIndexProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(Cache::class.java)
            .toProvider(CacheProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(Cluster::class.java)
            .to(SingleWorldCluster::class.java)

        bind(PlayerStore::class.java)
            .to(DummyPlayerStore::class.java)
    }
}
