package org.openrs2.archive.masterindex

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import io.netty.buffer.Unpooled
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.buffer.use
import java.nio.file.Files

public class ImportCommand : CliktCommand(name = "import") {
    private val input by argument().path(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        val injector = Guice.createInjector(ArchiveModule)
        val importer = injector.getInstance(MasterIndexImporter::class.java)

        Unpooled.wrappedBuffer(Files.readAllBytes(input)).use { buf ->
            importer.import(buf)
        }
    }
}
