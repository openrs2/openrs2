package dev.openrs2.archive.key

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import dev.openrs2.archive.ArchiveModule
import kotlinx.coroutines.runBlocking

public class ImportCommand : CliktCommand(name = "import") {
    private val input by argument().path(
        mustExist = true,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val importer = injector.getInstance(KeyImporter::class.java)

        importer.import(input)
    }
}
