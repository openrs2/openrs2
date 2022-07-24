package org.openrs2.archive.cache

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.openrs2.archive.cache.nxt.InitJs5RemoteConnection
import org.openrs2.archive.cache.nxt.Js5Request
import org.openrs2.archive.cache.nxt.Js5RequestEncoder
import org.openrs2.archive.cache.nxt.Js5Response
import org.openrs2.archive.cache.nxt.Js5ResponseDecoder
import org.openrs2.archive.cache.nxt.LoginResponse
import org.openrs2.archive.cache.nxt.MusicStreamClient
import org.openrs2.buffer.use
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.js5.downstream.XorDecoder
import kotlin.coroutines.Continuation

public class NxtJs5ChannelHandler(
    bootstrap: Bootstrap,
    scopeId: Int,
    gameId: Int,
    hostname: String,
    port: Int,
    buildMajor: Int,
    buildMinor: Int,
    lastMasterIndexId: Int?,
    continuation: Continuation<Unit>,
    importer: CacheImporter,
    private val token: String,
    private val languageId: Int,
    private val musicStreamClient: MusicStreamClient,
    private val maxMinorBuildAttempts: Int = 5
) : Js5ChannelHandler(
    bootstrap,
    scopeId,
    gameId,
    hostname,
    port,
    buildMajor,
    buildMinor,
    lastMasterIndexId,
    continuation,
    importer,
    MasterIndexFormat.LENGTHS,
    maxInFlightRequests = 500
) {
    private data class MusicRequest(val archive: Int, val group: Int, val version: Int, val checksum: Int)

    private var inFlightRequests = 0
    private val pendingRequests = ArrayDeque<MusicRequest>()
    private var scope: CoroutineScope? = null
    private var minorBuildAttempts = 0

    override fun createInitMessage(): Any {
        return InitJs5RemoteConnection(buildMajor, buildMinor!!, token, languageId)
    }

    override fun createRequestMessage(prefetch: Boolean, archive: Int, group: Int): Any {
        return Js5Request.Group(prefetch, archive, group, buildMajor)
    }

    override fun createConnectedMessage(): Any? {
        return Js5Request.Connected(buildMajor)
    }

    override fun configurePipeline(pipeline: ChannelPipeline) {
        pipeline.addBefore("handler", null, Js5RequestEncoder)
        pipeline.addBefore("handler", null, XorDecoder())
        pipeline.addBefore("handler", null, Js5ResponseDecoder())

        pipeline.remove(Rs2Encoder::class.java)
        pipeline.remove(Rs2Decoder::class.java)
    }

    override fun incrementVersion() {
        buildMinor = buildMinor!! + 1

        if (++minorBuildAttempts >= maxMinorBuildAttempts) {
            buildMajor++
            buildMinor = 1
        }
    }

    override fun channelActive(ctx: ChannelHandlerContext) {
        super.channelActive(ctx)
        scope = CoroutineScope(ctx.channel().eventLoop().asCoroutineDispatcher())
    }

    override fun channelInactive(ctx: ChannelHandlerContext) {
        super.channelInactive(ctx)
        scope!!.cancel()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is LoginResponse.Js5Ok -> handleOk(ctx)
            is LoginResponse.ClientOutOfDate -> handleClientOutOfDate(ctx)
            is LoginResponse -> throw Exception("Invalid response: $msg")
            is Js5Response -> handleResponse(ctx, msg.prefetch, msg.archive, msg.group, msg.data)
            else -> throw Exception("Unknown message type: ${msg.javaClass.name}")
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        super.channelReadComplete(ctx)

        while (inFlightRequests < 6) {
            val request = pendingRequests.removeFirstOrNull() ?: break
            inFlightRequests++

            logger.info { "Requesting archive ${request.archive} group ${request.group}" }

            scope!!.launch {
                val archive = request.archive
                val group = request.group
                val version = request.version
                val checksum = request.checksum

                musicStreamClient.request(archive, group, version, checksum, buildMajor).use { buf ->
                    inFlightRequests--

                    processResponse(ctx, archive, group, buf)

                    /*
                     * Inject a fake channelReadComplete event to ensure we
                     * don't time out and to send any new music requests.
                     */
                    ctx.channel().pipeline().fireChannelReadComplete()
                }
            }
        }
    }

    override fun isComplete(): Boolean {
        return super.isComplete() && pendingRequests.isEmpty() && inFlightRequests == 0
    }

    override fun request(ctx: ChannelHandlerContext, archive: Int, group: Int, version: Int, checksum: Int) {
        if (archive == MUSIC_ARCHIVE) {
            pendingRequests += MusicRequest(archive, group, version, checksum)
        } else {
            super.request(ctx, archive, group, version, checksum)
        }
    }

    private companion object {
        private val logger = InlineLogger()

        private const val MUSIC_ARCHIVE = 40
    }
}
