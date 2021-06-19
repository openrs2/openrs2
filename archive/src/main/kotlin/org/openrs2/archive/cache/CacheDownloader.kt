package org.openrs2.archive.cache

import org.openrs2.archive.game.GameDatabase
import org.openrs2.archive.jav.JavConfig
import org.openrs2.net.BootstrapFactory
import org.openrs2.net.awaitSuspend
import java.net.URI
import java.net.http.HttpClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

@Singleton
public class CacheDownloader @Inject constructor(
    private val client: HttpClient,
    private val bootstrapFactory: BootstrapFactory,
    private val gameDatabase: GameDatabase,
    private val importer: CacheImporter
) {
    public suspend fun download(gameName: String) {
        val game = gameDatabase.getGame(gameName) ?: throw Exception("Game not found")

        val url = game.url ?: throw Exception("URL not set")
        val build = game.build ?: throw Exception("Current build not set")

        val config = JavConfig.download(client, url)
        val codebase = config.config[CODEBASE] ?: throw Exception("Codebase missing")
        val hostname = URI(codebase).host ?: throw Exception("Hostname missing")

        val group = bootstrapFactory.createEventLoopGroup()
        try {
            suspendCoroutine<Unit> { continuation ->
                val bootstrap = bootstrapFactory.createBootstrap(group)
                val handler = Js5ChannelHandler(
                    bootstrap,
                    game.id,
                    hostname,
                    PORT,
                    build,
                    game.lastMasterIndexId,
                    continuation,
                    importer
                )

                bootstrap.handler(Js5ChannelInitializer(handler))
                    .connect(hostname, PORT)
            }
        } finally {
            group.shutdownGracefully().awaitSuspend()
        }
    }

    private companion object {
        private const val CODEBASE = "codebase"
        private const val PORT = 443
    }
}
