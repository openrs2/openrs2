package org.openrs2.archive

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import dev.openrs2.net.NetworkModule
import org.openrs2.buffer.BufferModule
import org.openrs2.db.Database
import org.openrs2.json.JsonModule

public object ArchiveModule : AbstractModule() {
    override fun configure() {
        install(BufferModule)
        install(NetworkModule)
        install(JsonModule)

        bind(Database::class.java)
            .toProvider(DatabaseProvider::class.java)
            .`in`(Scopes.SINGLETON)
    }
}
