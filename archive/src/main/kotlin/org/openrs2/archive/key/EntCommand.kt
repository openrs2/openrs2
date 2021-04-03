package org.openrs2.archive.key

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector
import java.io.BufferedOutputStream
import java.io.DataOutputStream

public class EntCommand : CliktCommand(name = "ent") {
    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val exporter = injector.getInstance(KeyExporter::class.java)
            val keys = exporter.exportValid()

            val process = ProcessBuilder("ent")
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()

            DataOutputStream(BufferedOutputStream(process.outputStream)).use { out ->
                for (key in keys) {
                    out.writeInt(key.k0)
                    out.writeInt(key.k1)
                    out.writeInt(key.k2)
                    out.writeInt(key.k3)
                }
            }

            val status = process.waitFor()
            if (status != 0) {
                throw Exception("ent failed: $status")
            }
        }
    }
}
