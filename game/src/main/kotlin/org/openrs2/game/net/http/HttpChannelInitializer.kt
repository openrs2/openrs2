package org.openrs2.game.net.http

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.timeout.IdleStateHandler
import jakarta.inject.Inject
import jakarta.inject.Singleton
import java.util.concurrent.TimeUnit

@Singleton
public class HttpChannelInitializer @Inject constructor(
    private val handler: HttpChannelHandler
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            IdleStateHandler(true, TIMEOUT_SECS, TIMEOUT_SECS, TIMEOUT_SECS, TimeUnit.SECONDS),
            HttpRequestDecoder(),
            HttpResponseEncoder(),
            HttpObjectAggregator(Http.MAX_CONTENT_LENGTH),
            handler
        )
    }

    private companion object {
        private const val TIMEOUT_SECS: Long = 30
    }
}
