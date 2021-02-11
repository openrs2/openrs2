package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.cache.DiskStore

public class ExportCommand : CliktCommand(name = "export") {
    private val id by argument().int()
    private val output by argument().path(
        mustExist = true,
        canBeFile = false,
        mustBeReadable = true,
        mustBeWritable = true
    )

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val exporter = injector.getInstance(CacheExporter::class.java)

        DiskStore.create(output).use { store ->
            exporter.export(id, store)
        }
    }
}
