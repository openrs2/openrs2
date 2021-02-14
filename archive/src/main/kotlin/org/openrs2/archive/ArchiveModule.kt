package org.openrs2.archive

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.openrs2.archive.key.KeyDownloader
import org.openrs2.archive.key.OpenOsrsKeyDownloader
import org.openrs2.archive.key.RuneLiteKeyDownloader
import org.openrs2.buffer.BufferModule
import org.openrs2.cache.CacheModule
import org.openrs2.db.Database
import org.openrs2.http.HttpModule
import org.openrs2.json.JsonModule
import org.openrs2.net.NetworkModule

public object ArchiveModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
        install(CacheModule)
        install(HttpModule)
        install(JsonModule)
        install(NetworkModule)

        bind(Database::class.java)
            .toProvider(DatabaseProvider::class.java)
            .`in`(Scopes.SINGLETON)

        val binder = Multibinder.newSetBinder(binder(), KeyDownloader::class.java)
        binder.addBinding().to(OpenOsrsKeyDownloader::class.java)
        binder.addBinding().to(RuneLiteKeyDownloader::class.java)
    }
}
