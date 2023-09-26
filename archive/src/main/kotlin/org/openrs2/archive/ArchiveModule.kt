package org.openrs2.archive

import com.fasterxml.jackson.databind.Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.google.inject.AbstractModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import org.openrs2.archive.key.HdosKeyDownloader
import org.openrs2.archive.key.KeyDownloader
import org.openrs2.archive.key.RuneLiteKeyDownloader
import org.openrs2.archive.name.NameDownloader
import org.openrs2.archive.name.RuneStarNameDownloader
import org.openrs2.asm.AsmModule
import org.openrs2.buffer.BufferModule
import org.openrs2.cache.CacheModule
import org.openrs2.db.Database
import org.openrs2.http.HttpModule
import org.openrs2.json.JsonModule
import org.openrs2.net.NetworkModule
import org.openrs2.yaml.YamlModule
import javax.sql.DataSource

public object ArchiveModule : AbstractModule() {
    override fun configure() {
        install(AsmModule)
        install(BufferModule)
        install(CacheModule)
        install(HttpModule)
        install(JsonModule)
        install(NetworkModule)
        install(YamlModule)

        bind(ArchiveConfig::class.java)
            .toProvider(ArchiveConfigProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(DataSource::class.java)
            .toProvider(DataSourceProvider::class.java)
            .`in`(Scopes.SINGLETON)

        bind(Database::class.java)
            .toProvider(DatabaseProvider::class.java)
            .`in`(Scopes.SINGLETON)

        Multibinder.newSetBinder(binder(), Module::class.java)
            .addBinding().to(JavaTimeModule::class.java)

        val keyBinder = Multibinder.newSetBinder(binder(), KeyDownloader::class.java)
        keyBinder.addBinding().to(HdosKeyDownloader::class.java)
        keyBinder.addBinding().to(RuneLiteKeyDownloader::class.java)

        val nameBinder = Multibinder.newSetBinder(binder(), NameDownloader::class.java)
        nameBinder.addBinding().to(RuneStarNameDownloader::class.java)
    }
}
