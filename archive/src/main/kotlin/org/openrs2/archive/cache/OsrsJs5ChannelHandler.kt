package org.openrs2.archive.cache

import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import org.openrs2.archive.cache.osrs.InitJs5RemoteConnection
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.crypto.SymmetricKey
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.js5.downstream.Js5LoginResponse
import org.openrs2.protocol.js5.downstream.Js5Response
import org.openrs2.protocol.js5.downstream.Js5ResponseDecoder
import org.openrs2.protocol.js5.downstream.XorDecoder
import org.openrs2.protocol.js5.upstream.Js5Request
import org.openrs2.protocol.js5.upstream.Js5RequestEncoder
import kotlin.coroutines.Continuation

public class OsrsJs5ChannelHandler(
    bootstrap: Bootstrap,
    scopeId: Int,
    gameId: Int,
    hostname: String,
    port: Int,
    build: Int,
    lastMasterIndexId: Int?,
    continuation: Continuation<Unit>,
    importer: CacheImporter
) : Js5ChannelHandler(
    bootstrap,
    scopeId,
    gameId,
    hostname,
    port,
    build,
    null,
    lastMasterIndexId,
    continuation,
    importer,
    MasterIndexFormat.VERSIONED,
    maxInFlightRequests = 200
) {
    override fun createInitMessage(): Any {
        return InitJs5RemoteConnection(buildMajor, SymmetricKey.ZERO)
    }

    override fun createRequestMessage(prefetch: Boolean, archive: Int, group: Int): Any {
        return Js5Request.Group(prefetch, archive, group)
    }

    override fun createConnectedMessage(): Any? {
        return null
    }

    override fun configurePipeline(pipeline: ChannelPipeline) {
        pipeline.addBefore("handler", null, Js5RequestEncoder)
        pipeline.addBefore("handler", null, XorDecoder())
        pipeline.addBefore("handler", null, Js5ResponseDecoder())

        pipeline.remove(Rs2Encoder::class.java)
        pipeline.remove(Rs2Decoder::class.java)
    }

    override fun incrementVersion() {
        buildMajor++
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is Js5LoginResponse.Ok -> handleOk(ctx)
            is Js5LoginResponse.ClientOutOfDate -> handleClientOutOfDate(ctx)
            is Js5LoginResponse -> throw Exception("Invalid response: $msg")
            is Js5Response -> handleResponse(ctx, msg.prefetch, msg.archive, msg.group, msg.data)
            else -> throw Exception("Unknown message type: ${msg.javaClass.name}")
        }
    }
}
