package org.openrs2.archive.web

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import org.openrs2.archive.ArchiveModule

public class WebCommand : CliktCommand(name = "web") {
    override fun run() {
        val injector = Guice.createInjector(ArchiveModule)
        val server = injector.getInstance(WebServer::class.java)
        server.start()
    }
}
