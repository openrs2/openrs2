package org.openrs2.game.net.http

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import org.openrs2.buffer.use
import org.openrs2.game.net.FileProvider
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@ChannelHandler.Sharable
public class HttpChannelHandler @Inject constructor(
    private val fileProvider: FileProvider
) : SimpleChannelInboundHandler<HttpRequest>() {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val uri = msg.uri()
        if (!uri.startsWith("/")) {
            Http.writeResponse(ctx, msg, HttpResponseStatus.BAD_REQUEST)
            return
        }

        fileProvider.get(uri.substring(1)).use { file ->
            if (file == null) {
                Http.writeResponse(ctx, msg, HttpResponseStatus.NOT_FOUND)
                return
            }

            Http.writeResponse(ctx, msg, file, HttpHeaderValues.APPLICATION_OCTET_STREAM)
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }
}
