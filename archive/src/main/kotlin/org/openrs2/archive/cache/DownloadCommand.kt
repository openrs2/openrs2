package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule

public class DownloadCommand : CliktCommand(name = "download") {
    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val downloader = injector.getInstance(CacheDownloader::class.java)
        // TODO(gpe): make these configurable and/or fetch from the database
        downloader.download("oldschool1.runescape.com", 43594, 193)
    }
}
