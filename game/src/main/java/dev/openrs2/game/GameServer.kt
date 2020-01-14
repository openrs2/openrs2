package dev.openrs2.game

import com.google.inject.Guice
import javax.inject.Singleton

fun main() {
    val injector = Guice.createInjector(GameModule())
    val server = injector.getInstance(GameServer::class.java)
    server.run()
}

@Singleton
class GameServer {
    fun run() {
        TODO()
    }
}
