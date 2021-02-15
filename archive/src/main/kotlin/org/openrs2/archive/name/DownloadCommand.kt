package org.openrs2.archive.name

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule

public class DownloadCommand : CliktCommand(name = "download") {
    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val importer = injector.getInstance(NameImporter::class.java)
        importer.download()
    }
}
