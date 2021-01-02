package org.openrs2.archive.cache

import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.runBlocking
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.js5.Js5Request
import org.openrs2.protocol.js5.Js5RequestEncoder
import org.openrs2.protocol.js5.Js5Response
import org.openrs2.protocol.js5.Js5ResponseDecoder
import org.openrs2.protocol.js5.XorDecoder
import org.openrs2.protocol.login.LoginRequest
import org.openrs2.protocol.login.LoginResponse
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ChannelHandler.Sharable
public class Js5ChannelHandler(
    private val bootstrap: Bootstrap,
    private val hostname: String,
    private val port: Int,
    private var version: Int,
    private val continuation: Continuation<Unit>,
    private val importer: CacheImporter,
    private val maxInFlightRequests: Int = 200,
    maxVersionAttempts: Int = 10
) : SimpleChannelInboundHandler<Any>(Object::class.java) {
    private val maxVersion = version + maxVersionAttempts
    private val inFlightRequests = mutableSetOf<Js5Request.Group>()
    private val pendingRequests = ArrayDeque<Js5Request.Group>()
    private lateinit var indexes: Array<Js5Index?>
    private val groups = mutableListOf<CacheImporter.Group>()

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(LoginRequest.InitJs5RemoteConnection(version), ctx.voidPromise())
        ctx.read()
    }

    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is LoginResponse.Js5Ok -> handleOk(ctx)
            is LoginResponse.ClientOutOfDate -> handleClientOutOfDate(ctx)
            is LoginResponse -> throw Exception("Invalid response: $msg")
            is Js5Response -> handleResponse(ctx, msg)
            else -> throw Exception("Unknown message type: ${msg.javaClass.name}")
        }
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        var flush = false

        while (inFlightRequests.size < maxInFlightRequests) {
            val request = pendingRequests.removeFirstOrNull() ?: break
            inFlightRequests += request
            ctx.write(request, ctx.voidPromise())

            flush = true
        }

        if (flush) {
            ctx.flush()
            ctx.read()
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        releaseGroups()
        ctx.close()
        continuation.resumeWithException(cause)
    }

    private fun handleOk(ctx: ChannelHandlerContext) {
        val pipeline = ctx.pipeline()

        pipeline.remove(Rs2Encoder::class.java)
        pipeline.remove(Rs2Decoder::class.java)
        pipeline.addFirst(
            Js5RequestEncoder,
            XorDecoder(),
            Js5ResponseDecoder()
        )

        val request = Js5Request.Group(false, Js5Archive.ARCHIVESET, Js5Archive.ARCHIVESET)
        pendingRequests += request
    }

    private fun handleClientOutOfDate(ctx: ChannelHandlerContext) {
        if (++version > maxVersion) {
            throw Exception("Failed to identify current version")
        }

        ctx.close()
        bootstrap.connect(hostname, port)
    }

    private fun handleResponse(ctx: ChannelHandlerContext, response: Js5Response) {
        val request = Js5Request.Group(response.prefetch, response.archive, response.group)

        val removed = inFlightRequests.remove(request)
        if (!removed) {
            throw Exception("Received response for request not in-flight")
        }

        if (response.archive == Js5Archive.ARCHIVESET && response.group == Js5Archive.ARCHIVESET) {
            processMasterIndex(response.data)
        } else if (response.archive == Js5Archive.ARCHIVESET) {
            processIndex(response.group, response.data)
        } else {
            val version = indexes[response.archive]!![response.group]!!.version
            val encrypted = Js5Compression.isEncrypted(response.data.slice())
            groups += CacheImporter.Group(response.archive, response.group, response.data.retain(), version, encrypted)
        }

        val complete = pendingRequests.isEmpty() && inFlightRequests.isEmpty()

        if (groups.size >= CacheImporter.BATCH_SIZE || complete) {
            runBlocking {
                importer.importGroups(groups)
            }

            releaseGroups()
        }

        if (complete) {
            ctx.close()
            continuation.resume(Unit)
        }
    }

    private fun processMasterIndex(buf: ByteBuf) {
        val masterIndex = Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            Js5MasterIndex.read(uncompressed)
        }

        val rawIndexes = runBlocking { importer.importMasterIndexAndGetIndexes(masterIndex, buf) }
        try {
            indexes = arrayOfNulls(rawIndexes.size)

            for ((archive, index) in rawIndexes.withIndex()) {
                if (index != null) {
                    processIndex(archive, index)
                } else {
                    pendingRequests += Js5Request.Group(false, Js5Archive.ARCHIVESET, archive)
                }
            }
        } finally {
            rawIndexes.filterNotNull().forEach(ByteBuf::release)
        }
    }

    private fun processIndex(archive: Int, buf: ByteBuf) {
        val index = Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            Js5Index.read(uncompressed)
        }
        indexes[archive] = index

        val groups = runBlocking {
            importer.importIndexAndGetMissingGroups(archive, index, buf)
        }
        for (group in groups) {
            pendingRequests += Js5Request.Group(false, archive, group)
        }
    }

    private fun releaseGroups() {
        groups.forEach(CacheImporter.Group::release)
        groups.clear()
    }
}
