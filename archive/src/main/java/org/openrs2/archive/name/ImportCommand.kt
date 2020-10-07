package org.openrs2.archive.name

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule

public class ImportCommand : CliktCommand(name = "import") {
    private val input by option().inputStream().defaultStdin()

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val importer = injector.getInstance(NameImporter::class.java)

        input.bufferedReader().useLines { lines ->
            importer.import(lines.toSet())
        }
    }
}
