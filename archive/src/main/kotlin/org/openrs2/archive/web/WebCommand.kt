package org.openrs2.archive.web

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.google.inject.Guice
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class WebCommand : CliktCommand(name = "web") {
    private val address by option().default("::")
    private val port by option().int().default(8080)

    override fun run() {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val server = injector.getInstance(WebServer::class.java)
            server.start(address, port)
        }
    }
}
