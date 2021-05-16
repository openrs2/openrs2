package org.openrs2.game

import com.github.ajalt.clikt.core.CliktCommand
import com.github.michaelbull.logging.InlineLogger
import com.google.inject.Guice
import org.openrs2.inject.CloseableInjector

public fun main(args: Array<String>): Unit = GameCommand().main(args)

public class GameCommand : CliktCommand(name = "game") {
    override fun run() {
        val start = System.nanoTime()
        logger.info { "Starting OpenRS2..." }

        CloseableInjector(Guice.createInjector(GameModule)).use { injector ->
            val server = injector.getInstance(GameServer::class.java)
            server.run(start)
        }
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
