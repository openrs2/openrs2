package org.openrs2.game.net

import com.google.common.util.concurrent.AbstractService
import io.netty.channel.EventLoopGroup
import io.netty.channel.MultiThreadIoEventLoopGroup
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.game.net.http.HttpChannelInitializer
import org.openrs2.net.BootstrapFactory
import org.openrs2.net.Transport
import org.openrs2.net.asCompletableFuture
import java.util.concurrent.CompletableFuture

@Singleton
public class NetworkService @Inject constructor(
    private val transport: Transport,
    private val bootstrapFactory: BootstrapFactory,
    private val httpInitializer: HttpChannelInitializer,
    private val rs2Initializer: Rs2ChannelInitializer
) : AbstractService() {
    private lateinit var group: EventLoopGroup

    override fun doStart() {
        group = MultiThreadIoEventLoopGroup(transport.ioHandlerFactory)

        val httpFuture = bootstrapFactory.createServerBootstrap(group, transport.serverSocketChannel)
            .childHandler(httpInitializer)
            .bind(HTTP_PORT)
            .asCompletableFuture()

        val rs2Initializer = bootstrapFactory.createServerBootstrap(group, transport.serverSocketChannel)
            .childHandler(rs2Initializer)

        val rs2PrimaryFuture = rs2Initializer.bind(RS2_PRIMARY_PORT)
            .asCompletableFuture()

        val rs2SecondaryFuture = rs2Initializer.bind(RS2_SECONDARY_PORT)
            .asCompletableFuture()

        CompletableFuture.allOf(httpFuture, rs2PrimaryFuture, rs2SecondaryFuture).handle { _, ex ->
            if (ex != null) {
                group.shutdownGracefully()
                notifyFailed(ex)
            } else {
                notifyStarted()
            }
        }
    }

    override fun doStop() {
        group.shutdownGracefully().addListener { future ->
            if (future.isSuccess) {
                notifyStopped()
            } else {
                notifyFailed(future.cause())
            }
        }
    }

    private companion object {
        // TODO(gpe): make these configurable
        private const val RS2_PRIMARY_PORT = 40001
        private const val RS2_SECONDARY_PORT = 50001
        private const val HTTP_PORT = 7001
    }
}
