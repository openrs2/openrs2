package org.openrs2.archive.web

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import org.openrs2.archive.ArchiveModule
import org.openrs2.inject.CloseableInjector

public class WebCommand : CliktCommand(name = "web") {
    override fun run() {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val server = injector.getInstance(WebServer::class.java)
            server.start()
        }
    }
}
