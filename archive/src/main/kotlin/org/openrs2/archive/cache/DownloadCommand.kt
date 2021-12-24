package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class DownloadCommand : CliktCommand(name = "download") {
    private val environment by option().default("live")
    private val language by option().default("en")

    private val game by argument().default("oldschool")

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val downloader = injector.getInstance(CacheDownloader::class.java)
            downloader.download(game, environment, language)
        }
    }
}
