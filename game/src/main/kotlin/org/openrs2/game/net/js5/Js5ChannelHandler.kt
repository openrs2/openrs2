package org.openrs2.game.net.js5

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleStateEvent
import org.openrs2.protocol.js5.Js5Request
import org.openrs2.protocol.js5.XorEncoder
import javax.inject.Inject

public class Js5ChannelHandler @Inject constructor(
    private val service: Js5Service
) : SimpleChannelInboundHandler<Js5Request>() {
    private lateinit var client: Js5Client

    override fun handlerAdded(ctx: ChannelHandlerContext) {
        client = Js5Client(ctx.read())
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Js5Request) {
        when (msg) {
            is Js5Request.Group -> service.push(client, msg)
            is Js5Request.Rekey -> handleRekey(ctx, msg)
            is Js5Request.Disconnect -> ctx.close()
        }
    }

    private fun handleRekey(ctx: ChannelHandlerContext, msg: Js5Request.Rekey) {
        val encoder = ctx.pipeline().get(XorEncoder::class.java)
        encoder.key = msg.key
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        service.readIfNotFull(client)
    }

    override fun channelWritabilityChanged(ctx: ChannelHandlerContext) {
        if (ctx.channel().isWritable) {
            service.notifyIfNotEmpty(client)
        }
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            ctx.close()
        }
    }
}
