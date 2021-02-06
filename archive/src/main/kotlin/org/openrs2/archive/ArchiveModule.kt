package org.openrs2.archive

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import org.openrs2.buffer.BufferModule
import org.openrs2.cache.CacheModule
import org.openrs2.db.Database
import org.openrs2.json.JsonModule
import org.openrs2.net.NetworkModule

public object ArchiveModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
        install(CacheModule)
        install(JsonModule)
        install(NetworkModule)

        bind(Database::class.java)
            .toProvider(DatabaseProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
