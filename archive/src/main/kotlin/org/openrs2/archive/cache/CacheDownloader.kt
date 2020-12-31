package org.openrs2.archive.cache

import dev.openrs2.net.BootstrapFactory
import dev.openrs2.net.suspend
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.suspendCoroutine

@Singleton
public class CacheDownloader @Inject constructor(
    private val bootstrapFactory: BootstrapFactory,
    private val importer: CacheImporter
) {
    public suspend fun download(hostname: String, port: Int, version: Int) {
        val group = bootstrapFactory.createEventLoopGroup()
        try {
            suspendCoroutine<Unit> { continuation ->
                val bootstrap = bootstrapFactory.createBootstrap(group)
                val handler = Js5ChannelHandler(bootstrap, hostname, port, version, continuation, importer)

                bootstrap.handler(Js5ChannelInitializer(handler))
                    .connect(hostname, port)
            }
        } finally {
            group.shutdownGracefully().suspend()
        }
    }
}
