package org.openrs2.archive.cache

import org.openrs2.archive.game.GameDatabase
import org.openrs2.net.BootstrapFactory
import org.openrs2.net.awaitSuspend
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

@Singleton
public class CacheDownloader @Inject constructor(
    private val bootstrapFactory: BootstrapFactory,
    private val gameDatabase: GameDatabase,
    private val importer: CacheImporter
) {
    public suspend fun download(gameName: String) {
        val game = gameDatabase.getGame(gameName) ?: throw Exception("Game not found")

        val hostname = game.hostname ?: throw Exception("Hostname not set")
        val port = game.port ?: throw Exception("Port not set")
        val build = game.build ?: throw Exception("Current build not set")

        val group = bootstrapFactory.createEventLoopGroup()
        try {
            suspendCoroutine<Unit> { continuation ->
                val bootstrap = bootstrapFactory.createBootstrap(group)
                val handler = Js5ChannelHandler(
                    bootstrap,
                    game.id,
                    hostname,
                    port,
                    build,
                    game.previousMasterIndexId,
                    continuation,
                    importer
                )

                bootstrap.handler(Js5ChannelInitializer(handler))
                    .connect(hostname, port)
            }
        } finally {
            group.shutdownGracefully().awaitSuspend()
        }
    }
}
