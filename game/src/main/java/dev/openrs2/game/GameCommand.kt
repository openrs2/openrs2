package dev.openrs2.game

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice

public fun main(args: Array<String>): Unit = GameCommand().main(args)

public class GameCommand : CliktCommand(name = "game") {
    override fun run() {
        val injector = Guice.createInjector(GameModule)
        val server = injector.getInstance(GameServer::class.java)
        server.run()
    }
}
