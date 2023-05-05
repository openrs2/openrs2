package org.openrs2.archive.cache

import org.openrs2.archive.cache.nxt.MusicStreamClient
import org.openrs2.archive.game.GameDatabase
import org.openrs2.archive.jav.JavConfig
import org.openrs2.buffer.ByteBufBodyHandler
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
    private val byteBufBodyHandler: ByteBufBodyHandler,
    private val bootstrapFactory: BootstrapFactory,
    private val gameDatabase: GameDatabase,
    private val importer: CacheImporter
) {
    public suspend fun download(gameName: String, environment: String, language: String) {
        val game = gameDatabase.getGame(gameName, environment, language) ?: throw Exception("Game not found")

        val url = game.url ?: throw Exception("URL not set")
        val buildMajor = game.buildMajor ?: throw Exception("Current major build not set")

        val config = JavConfig.download(client, url)

        val group = bootstrapFactory.createEventLoopGroup()
        try {
            suspendCoroutine<Unit> { continuation ->
                val bootstrap = bootstrapFactory.createBootstrap(group)

                val hostname: String

                val initializer = when (gameName) {
                    "oldschool" -> {
                        val codebase = config.config[CODEBASE] ?: throw Exception("Codebase missing")
                        hostname = URI(codebase).host ?: throw Exception("Hostname missing")

                        OsrsJs5ChannelInitializer(
                            OsrsJs5ChannelHandler(
                                bootstrap,
                                game.scopeId,
                                game.id,
                                hostname,
                                PORT,
                                buildMajor,
                                game.lastMasterIndexId,
                                continuation,
                                importer
                            )
                        )
                    }

                    "runescape" -> {
                        val buildMinor = game.buildMinor ?: throw Exception("Current minor build not set")

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
                                buildMajor,
                                buildMinor,
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

    private companion object {
        private const val CODEBASE = "codebase"
        private const val NXT_LIVE_HOSTNAME = "content.runescape.com"
        private const val NXT_BETA_HOSTNAME = "content.beta.runescape.com"
        private const val PORT = 443
        private val TOKEN_REGEX = Regex("[A-Za-z0-9*-]{32}")
    }
}
