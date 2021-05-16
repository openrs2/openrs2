package org.openrs2.game.net.http

import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class HttpChannelInitializer @Inject constructor(
    private val handler: HttpChannelHandler
) : ChannelInitializer<Channel>() {
    override fun initChannel(ch: Channel) {
        ch.pipeline().addLast(
            HttpRequestDecoder(),
            HttpResponseEncoder(),
            HttpObjectAggregator(MAX_CONTENT_LENGTH),
            handler
        )
    }

    private companion object {
        private const val MAX_CONTENT_LENGTH = 65536
    }
}
