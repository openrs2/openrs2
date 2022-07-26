package org.openrs2.game.net.login

import io.netty.buffer.Unpooled
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.DelimiterBasedFrameDecoder
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.HttpRequestDecoder
import io.netty.handler.codec.http.HttpResponseEncoder
import io.netty.handler.codec.string.StringDecoder
import io.netty.handler.timeout.IdleStateEvent
import org.openrs2.buffer.copiedBuffer
import org.openrs2.game.cluster.Cluster
import org.openrs2.game.net.crossdomain.CrossDomainChannelHandler
import org.openrs2.game.net.http.Http
import org.openrs2.game.net.jaggrab.JaggrabChannelHandler
import org.openrs2.game.net.js5.Js5ChannelHandler
import org.openrs2.protocol.Protocol
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.jaggrab.upstream.JaggrabRequestDecoder
import org.openrs2.protocol.js5.downstream.Js5LoginResponse
import org.openrs2.protocol.js5.downstream.Js5RemoteDownstream
import org.openrs2.protocol.js5.downstream.Js5ResponseEncoder
import org.openrs2.protocol.js5.downstream.XorDecoder
import org.openrs2.protocol.js5.upstream.Js5RequestDecoder
import org.openrs2.protocol.login.upstream.LoginRequest
import org.openrs2.protocol.world.downstream.WorldListDownstream
import org.openrs2.protocol.world.downstream.WorldListResponse
import javax.inject.Inject
import javax.inject.Provider

public class LoginChannelHandler @Inject constructor(
    private val cluster: Cluster,
    private val js5HandlerProvider: Provider<Js5ChannelHandler>,
    private val jaggrabHandler: JaggrabChannelHandler,
    @Js5RemoteDownstream
    private val js5RemoteDownstreamProtocol: Protocol,
    @WorldListDownstream
    private val worldListDownstreamProtocol: Protocol
) : SimpleChannelInboundHandler<LoginRequest>(LoginRequest::class.java) {
    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: LoginRequest) {
        when (msg) {
            is LoginRequest.InitJs5RemoteConnection -> handleInitJs5RemoteConnection(ctx, msg)
            is LoginRequest.InitJaggrabConnection -> handleInitJaggrabConnection(ctx)
            is LoginRequest.RequestWorldList -> handleRequestWorldList(ctx, msg)
            is LoginRequest.InitCrossDomainConnection -> handleInitCrossDomainConnection(ctx)
            else -> Unit
        }
    }

    private fun handleInitJs5RemoteConnection(ctx: ChannelHandlerContext, msg: LoginRequest.InitJs5RemoteConnection) {
        val encoder = ctx.pipeline().get(Rs2Encoder::class.java)
        encoder.protocol = js5RemoteDownstreamProtocol

        if (msg.build != BUILD) {
            ctx.write(Js5LoginResponse.ClientOutOfDate).addListener(ChannelFutureListener.CLOSE)
            return
        }

        ctx.pipeline().addLast(
            XorDecoder(),
            Js5RequestDecoder(),
            Js5ResponseEncoder,
            js5HandlerProvider.get()
        )
        ctx.pipeline().remove(Rs2Decoder::class.java)

        ctx.write(Js5LoginResponse.Ok).addListener { future ->
            if (future.isSuccess) {
                ctx.pipeline().remove(encoder)
                ctx.pipeline().remove(this)
            }
        }
    }

    private fun handleInitJaggrabConnection(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast(
            DelimiterBasedFrameDecoder(JAGGRAB_MAX_FRAME_LENGTH, JAGGRAB_DELIMITER),
            StringDecoder(Charsets.UTF_8),
            JaggrabRequestDecoder,
            jaggrabHandler
        )
        ctx.pipeline().remove(Rs2Decoder::class.java)
        ctx.pipeline().remove(Rs2Encoder::class.java)
        ctx.pipeline().remove(this)
    }

    private fun handleRequestWorldList(ctx: ChannelHandlerContext, msg: LoginRequest.RequestWorldList) {
        val (worlds, players) = cluster.getWorldList()

        var checksum = worlds.hashCode()

        /*
         * Fix a 1 in 2^32 chance that we'll fail to fetch the original world
         * list on startup.
         */
        if (checksum == 0) {
            checksum = 1
        }

        val worldList = if (checksum != msg.checksum) {
            WorldListResponse.WorldList(worlds, checksum)
        } else {
            null
        }

        val encoder = ctx.pipeline().get(Rs2Encoder::class.java)
        encoder.protocol = worldListDownstreamProtocol

        ctx.write(WorldListResponse(worldList, players)).addListener(ChannelFutureListener.CLOSE)
    }

    private fun handleInitCrossDomainConnection(ctx: ChannelHandlerContext) {
        ctx.pipeline().addLast(
            HttpRequestDecoder(),
            HttpResponseEncoder(),
            HttpObjectAggregator(Http.MAX_CONTENT_LENGTH),
            CrossDomainChannelHandler
        )

        ctx.fireChannelRead(Unpooled.wrappedBuffer(G))

        ctx.pipeline().remove(Rs2Decoder::class.java)
        ctx.pipeline().remove(Rs2Encoder::class.java)
        ctx.pipeline().remove(this)
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        ctx.flush()
    }

    override fun userEventTriggered(ctx: ChannelHandlerContext, evt: Any) {
        if (evt is IdleStateEvent) {
            ctx.close()
        }
    }

    private companion object {
        private const val BUILD = 550
        private const val JAGGRAB_MAX_FRAME_LENGTH = 4096
        private val JAGGRAB_DELIMITER = Unpooled.unreleasableBuffer(copiedBuffer("\n\n"))
        private val G = byteArrayOf('G'.code.toByte())
    }
}
