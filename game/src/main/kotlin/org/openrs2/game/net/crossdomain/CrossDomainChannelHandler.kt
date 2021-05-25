package org.openrs2.game.net.crossdomain

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import org.openrs2.game.net.http.Http

@ChannelHandler.Sharable
public object CrossDomainChannelHandler : SimpleChannelInboundHandler<HttpRequest>() {
    private const val ENDPOINT = "/crossdomain.xml"
    private val POLICY = """
        <?xml version="1.0"?>
        <!DOCTYPE cross-domain-policy SYSTEM "http://www.adobe.com/xml/dtds/cross-domain-policy.dtd">
        <cross-domain-policy>
            <site-control permitted-cross-domain-policies="master-only" />
            <allow-access-from domain="*" />
        </cross-domain-policy>
    """.trimIndent().plus("\n").toByteArray(Charsets.UTF_8)

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: HttpRequest) {
        if (msg.method() != HttpMethod.GET || msg.uri() != ENDPOINT) {
            ctx.write(Http.createResponse(HttpResponseStatus.BAD_REQUEST)).addListener(ChannelFutureListener.CLOSE)
            return
        }

        val response = Http.createResponse(HttpResponseStatus.OK)
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, Http.TEXT_X_CROSS_DOMAIN_POLICY)
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, POLICY.size)
        ctx.write(response, ctx.voidPromise())
        ctx.write(Unpooled.wrappedBuffer(POLICY)).addListener(ChannelFutureListener.CLOSE)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }
}
