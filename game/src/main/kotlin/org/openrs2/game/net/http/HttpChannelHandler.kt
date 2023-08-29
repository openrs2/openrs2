package org.openrs2.game.net.http

import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.timeout.IdleStateEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.buffer.use
import org.openrs2.game.net.FileProvider

@Singleton
@ChannelHandler.Sharable
public class HttpChannelHandler @Inject constructor(
    private val fileProvider: FileProvider
) : SimpleChannelInboundHandler<HttpRequest>(HttpRequest::class.java) {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        val uri = msg.uri()
        if (!uri.startsWith("/")) {
            Http.writeResponse(ctx, msg, HttpResponseStatus.BAD_REQUEST)
            return
        }

        if (msg.method() != HttpMethod.HEAD && msg.method() != HttpMethod.GET) {
            Http.writeResponse(ctx, msg, HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        }

        fileProvider.get(uri.substring(1)).use { file ->
            if (file == null) {
                Http.writeResponse(ctx, msg, HttpResponseStatus.NOT_FOUND)
                return
            }

            Http.writeResponse(ctx, msg, file, getContentType(uri))
        }
    }

    private fun getContentType(uri: String): CharSequence {
        /*
         * It doesn't make sense to return a MIME type for some of the files:
         *
         * - The .pack200 files are missing two bytes in the header.
         * - The .lib and .pack files are probably compressed with DEFLATE in
         *   nowrap mode.
         */
        return when {
            uri.endsWith(".jar") -> Http.APPLICATION_JAVA_ARCHIVE
            else -> HttpHeaderValues.APPLICATION_OCTET_STREAM
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()

        if (ctx.channel().isWritable) {
            ctx.read()
        }
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        if (ctx.channel().isWritable) {
            ctx.read()
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            ctx.close()
        }
    }
}
