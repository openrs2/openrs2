package org.openrs2.game.net.http

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
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
        val file = fileProvider.get(msg.uri())
        if (file == null) {
            ctx.write(createResponse(HttpResponseStatus.NOT_FOUND)).addListener(ChannelFutureListener.CLOSE)
            return
        }

        val response = createResponse(HttpResponseStatus.OK)
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_OCTET_STREAM)
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, file.count())

        ctx.write(response, ctx.voidPromise())
        ctx.write(file).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    private fun createResponse(status: HttpResponseStatus): HttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
        response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        response.headers().add(HttpHeaderNames.SERVER, "OpenRS2")
        return response
    }
}
