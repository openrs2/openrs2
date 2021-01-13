package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule

public class DownloadCommand : CliktCommand(name = "download") {
    private val game by argument().default("oldschool")

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val downloader = injector.getInstance(CacheDownloader::class.java)
        downloader.download(game)
    }
}
