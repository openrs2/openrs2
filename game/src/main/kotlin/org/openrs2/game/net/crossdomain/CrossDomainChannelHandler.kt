package org.openrs2.game.net.crossdomain

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.timeout.IdleStateEvent
import org.openrs2.buffer.use
import org.openrs2.game.net.http.Http

@ChannelHandler.Sharable
public object CrossDomainChannelHandler : SimpleChannelInboundHandler<HttpRequest>(HttpRequest::class.java) {
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
        // forcibly close the connection as we only expect a single request
        msg.headers().set(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)

        if (msg.method() != HttpMethod.GET) {
            Http.writeResponse(ctx, msg, HttpResponseStatus.METHOD_NOT_ALLOWED)
            return
        } else if (msg.uri() != ENDPOINT) {
            Http.writeResponse(ctx, msg, HttpResponseStatus.NOT_FOUND)
            return
        }

        Unpooled.wrappedBuffer(POLICY).use { buf ->
            Http.writeResponse(ctx, msg, buf, Http.TEXT_X_CROSS_DOMAIN_POLICY)
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            ctx.close()
        }
    }
}
