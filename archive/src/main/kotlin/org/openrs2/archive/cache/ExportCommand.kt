package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.cache.DiskStore
import org.openrs2.inject.CloseableInjector

public class ExportCommand : CliktCommand(name = "export") {
    private val scope by option().default("runescape")
    private val id by argument().int()
    private val output by argument().path(
        mustExist = true,
        canBeFile = false,
        mustBeReadable = true,
        mustBeWritable = true
    )

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val exporter = injector.getInstance(CacheExporter::class.java)

            exporter.export(scope, id) { legacy ->
                DiskStore.create(output, legacy = legacy)
            }
        }
    }
}
