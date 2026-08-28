package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import io.netty.buffer.Unpooled
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.buffer.use
import org.openrs2.inject.CloseableInjector
import kotlin.io.path.readBytes

public class ImportArchiveCommand : CliktCommand(name = "import-archive") {
    private val name by option()
    private val description by option()
    private val url by option()

    private val archive by argument()
    private val input by argument().path(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val importer = injector.getInstance(CacheImporter::class.java)

            val archiveId = ARCHIVES[archive] ?: archive.toIntOrNull() ?: throw IllegalArgumentException("Invalid archive ID")
            if (archiveId == 0) {
                throw IllegalArgumentException("Use ImportChecksumTableCommand instead")
            }

            Unpooled.wrappedBuffer(input.readBytes()).use { buf ->
                importer.importArchive(buf, archiveId, name, description, url)
            }
        }
    }

    private companion object {
        private val ARCHIVES = mapOf(
            "crc" to 0,
            "title" to 1,
            "config" to 2,
            "interface" to 3,
            "media" to 4,
            "versionlist" to 5,
            "textures" to 6,
            "wordenc" to 7,
            "sounds" to 8,
        )
    }
}
