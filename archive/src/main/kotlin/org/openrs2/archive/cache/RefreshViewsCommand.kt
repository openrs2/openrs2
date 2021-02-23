package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class RefreshViewsCommand : CliktCommand(name = "refresh-views") {
    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val importer = injector.getInstance(CacheImporter::class.java)
            importer.refreshViews()
        }
    }
}
