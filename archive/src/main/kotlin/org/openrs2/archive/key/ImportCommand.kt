package org.openrs2.archive.key

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class ImportCommand : CliktCommand(name = "import") {
    private val input by argument().path(
        mustExist = true,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val importer = injector.getInstance(KeyImporter::class.java)
            importer.import(input)
        }
    }
}
