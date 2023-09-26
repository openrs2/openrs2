package org.openrs2.archive.client

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.outputStream
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector
import java.io.FileNotFoundException

public class ExportCommand : CliktCommand(name = "export") {
    private val id by argument().long()
    private val output by argument().outputStream().defaultStdout()

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val exporter = injector.getInstance(ClientExporter::class.java)
            val artifact = exporter.export(id) ?: throw FileNotFoundException()
            try {
                val buf = artifact.content()
                buf.readBytes(output, buf.readableBytes())
            } finally {
                artifact.release()
            }
        }
    }
}
