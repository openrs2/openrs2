package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import io.netty.buffer.Unpooled
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.buffer.use
import org.openrs2.cli.instant
import java.nio.file.Files

public class ImportMasterIndexCommand : CliktCommand(name = "import-master-index") {
    private val build by option().int()
    private val timestamp by option().instant()
    private val name by option()
    private val description by option()

    private val game by argument()
    private val input by argument().path(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val importer = injector.getInstance(CacheImporter::class.java)

        Unpooled.wrappedBuffer(Files.readAllBytes(input)).use { buf ->
            importer.importMasterIndex(buf, game, build, timestamp, name, description)
        }
    }
}
