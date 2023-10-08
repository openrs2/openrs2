package org.openrs2.archive.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.multiple
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class ImportCommand : CliktCommand(name = "import") {
    private val name by option()
    private val description by option()
    private val url by option()

    private val input by argument().path(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true,
    ).multiple()

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val importer = injector.getInstance(ClientImporter::class.java)
            importer.import(input, name, description, url)
        }
    }
}
