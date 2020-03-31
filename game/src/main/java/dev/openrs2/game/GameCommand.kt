package dev.openrs2.game

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice

fun main(args: Array<String>) = GameCommand().main(args)

class GameCommand : CliktCommand(name = "game") {
    override fun run() {
        val injector = Guice.createInjector(GameModule())
        val server = injector.getInstance(GameServer::class.java)
        server.run()
    }
}
