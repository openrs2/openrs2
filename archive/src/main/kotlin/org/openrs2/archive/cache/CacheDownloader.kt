package org.openrs2.archive.cache

import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.archive.cache.nxt.MusicStreamClient
import org.openrs2.archive.game.GameDatabase
import org.openrs2.archive.jav.JavConfig
import org.openrs2.archive.world.World
import org.openrs2.archive.world.WorldList
import org.openrs2.buffer.ByteBufBodyHandler
import org.openrs2.buffer.use
import org.openrs2.net.BootstrapFactory
import org.openrs2.net.awaitSuspend
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import kotlin.coroutines.suspendCoroutine

@Singleton
public class CacheDownloader @Inject constructor(
    private val client: HttpClient,
    private val byteBufBodyHandler: ByteBufBodyHandler,
    private val bootstrapFactory: BootstrapFactory,
    private val gameDatabase: GameDatabase,
    private val importer: CacheImporter
) {
    public suspend fun download(gameName: String, environment: String, language: String) {
        val game = gameDatabase.getGame(gameName, environment, language) ?: throw Exception("Game not found")
        val url = game.url ?: throw Exception("URL not set")
        val config = JavConfig.download(client, url)

        val group = bootstrapFactory.createEventLoopGroup()
        try {
            suspendCoroutine { continuation ->
                val bootstrap = bootstrapFactory.createBootstrap(group)
                val hostname: String

                val initializer = when (gameName) {
                    "oldschool" -> {
                        var buildMajor = game.buildMajor

                        hostname = if (environment == "beta") {
                            findOsrsWorld(config, World::isBeta) ?: throw Exception("Failed to find beta world")
                        } else {
                            val codebase = config.config[CODEBASE] ?: throw Exception("Codebase missing")
                            URI(codebase).host ?: throw Exception("Hostname missing")
                        }

                        val serverVersion = config.params[OSRS_SERVER_VERSION]
                        if (serverVersion != null) {
                            buildMajor = serverVersion.toInt()
                        }

                        OsrsJs5ChannelInitializer(
                            OsrsJs5ChannelHandler(
                                bootstrap,
                                game.scopeId,
                                game.id,
                                hostname,
                                PORT,
                                buildMajor ?: throw Exception("Current major build not set"),
                                game.lastMasterIndexId,
                                continuation,
                                importer
                            )
                        )
                    }

                    "runescape" -> {
                        var buildMajor = game.buildMajor
                        var buildMinor = game.buildMinor

                        val serverVersion = config.config[NXT_SERVER_VERSION]
                        if (serverVersion != null) {
                            val n = serverVersion.toInt()

                            /*
                             * Only reset buildMinor if buildMajor changes, so
                             * we don't have to keep retrying minor versions.
                             */
                            if (buildMajor != n) {
                                buildMajor = n
                                buildMinor = 1
                            }
                        }

                        val tokens = config.params.values.filter { TOKEN_REGEX.matches(it) }
                        val token = tokens.singleOrNull() ?: throw Exception("Multiple candidate tokens: $tokens")

                        hostname = if (environment == "beta") {
                            NXT_BETA_HOSTNAME
                        } else {
                            NXT_LIVE_HOSTNAME
                        }

                        val musicStreamClient = MusicStreamClient(client, byteBufBodyHandler, "http://$hostname")

                        NxtJs5ChannelInitializer(
                            NxtJs5ChannelHandler(
                                bootstrap,
                                game.scopeId,
                                game.id,
                                hostname,
                                PORT,
                                buildMajor ?: throw Exception("Current major build not set"),
                                buildMinor ?: throw Exception("Current minor build not set"),
                                game.lastMasterIndexId,
                                continuation,
                                importer,
                                token,
                                game.languageId,
                                musicStreamClient
                            )
                        )
                    }

                    else -> throw UnsupportedOperationException()
                }

                bootstrap.handler(initializer)
                    .connect(hostname, PORT)
            }
        } finally {
            group.shutdownGracefully().awaitSuspend()
        }
    }

    private fun findOsrsWorld(config: JavConfig, predicate: (World) -> Boolean): String? {
        val url = config.params[OSRS_WORLD_LIST_URL] ?: throw Exception("World list URL missing")

        val list = client.send(HttpRequest.newBuilder(URI(url)).build(), byteBufBodyHandler).body().use { buf ->
            WorldList.read(buf)
        }

        return list.worlds
            .filter(predicate)
            .map(World::hostname)
            .shuffled()
            .firstOrNull()
    }

    private companion object {
        private const val CODEBASE = "codebase"
        private const val OSRS_WORLD_LIST_URL = "17"
        private const val OSRS_SERVER_VERSION = "25"
        private const val NXT_SERVER_VERSION = "server_version"
        private const val NXT_LIVE_HOSTNAME = "content.runescape.com"
        private const val NXT_BETA_HOSTNAME = "content.beta.runescape.com"
        private const val PORT = 443
        private val TOKEN_REGEX = Regex("[A-Za-z0-9*-]{32}")
    }
}
