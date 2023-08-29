package org.openrs2.game.net.jaggrab

import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.timeout.IdleStateEvent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.buffer.use
import org.openrs2.game.net.FileProvider
import org.openrs2.protocol.jaggrab.upstream.JaggrabRequest

@Singleton
@ChannelHandler.Sharable
public class JaggrabChannelHandler @Inject constructor(
    private val fileProvider: FileProvider
) : SimpleChannelInboundHandler<JaggrabRequest>(JaggrabRequest::class.java) {
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: JaggrabRequest) {
        fileProvider.get(msg.path).use { file ->
            if (file == null) {
                ctx.close()
                return
            }

            ctx.write(file.retain()).addListener(ChannelFutureListener.CLOSE)
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
