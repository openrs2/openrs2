package org.openrs2.archive.cache

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.runBlocking
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.protocol.Rs2Decoder
import org.openrs2.protocol.Rs2Encoder
import org.openrs2.protocol.js5.Js5Request
import org.openrs2.protocol.js5.Js5RequestEncoder
import org.openrs2.protocol.js5.Js5Response
import org.openrs2.protocol.js5.Js5ResponseDecoder
import org.openrs2.protocol.js5.XorDecoder
import org.openrs2.protocol.login.LoginRequest
import org.openrs2.protocol.login.LoginResponse
import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ChannelHandler.Sharable
public class Js5ChannelHandler(
    private val bootstrap: Bootstrap,
    private val gameId: Int,
    private val hostname: String,
    private val port: Int,
    private var build: Int,
    private val continuation: Continuation<Unit>,
    private val importer: CacheImporter,
    private val masterIndexFormat: MasterIndexFormat = MasterIndexFormat.VERSIONED,
    private val maxInFlightRequests: Int = 200,
    maxBuildAttempts: Int = 10
) : SimpleChannelInboundHandler<Any>(Object::class.java) {
    private val maxBuild = build + maxBuildAttempts
    private val inFlightRequests = mutableSetOf<Js5Request.Group>()
    private val pendingRequests = ArrayDeque<Js5Request.Group>()
    private var masterIndex: Js5MasterIndex? = null
    private lateinit var indexes: Array<Js5Index?>
    private val groups = mutableListOf<CacheImporter.Group>()

    override fun channelActive(ctx: ChannelHandlerContext) {
        ctx.writeAndFlush(LoginRequest.InitJs5RemoteConnection(build), ctx.voidPromise())
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

            logger.info { "Requesting archive ${request.archive} group ${request.group}" }
            ctx.write(request, ctx.voidPromise())

            flush = true
        }

        if (flush) {
            ctx.flush()
        }

        if (inFlightRequests.isNotEmpty()) {
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

        request(Js5Archive.ARCHIVESET, Js5Archive.ARCHIVESET)
    }

    private fun handleClientOutOfDate(ctx: ChannelHandlerContext) {
        if (++build > maxBuild) {
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
            val entry = indexes[response.archive]!![response.group]!!

            if (response.data.crc32() != entry.checksum) {
                throw Exception("Group checksum invalid")
            }

            groups += CacheImporter.Group(
                response.archive,
                response.group,
                response.data.retain(),
                entry.version,
                versionTruncated = false,
                Js5Compression.isEncrypted(response.data.slice())
            )
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
        masterIndex = Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            Js5MasterIndex.read(uncompressed, masterIndexFormat)
        }

        val rawIndexes = runBlocking {
            val name = "Downloaded from $hostname:$port"
            importer.importMasterIndexAndGetIndexes(masterIndex!!, buf, gameId, build, Instant.now(), name)
        }
        try {
            indexes = arrayOfNulls(rawIndexes.size)

            for ((archive, index) in rawIndexes.withIndex()) {
                if (index != null) {
                    processIndex(archive, index)
                } else {
                    request(Js5Archive.ARCHIVESET, archive)
                }
            }
        } finally {
            rawIndexes.filterNotNull().forEach(ByteBuf::release)
        }
    }

    private fun processIndex(archive: Int, buf: ByteBuf) {
        val entry = masterIndex!!.entries[archive]
        if (buf.crc32() != entry.checksum) {
            throw Exception("Index checksum invalid")
        }

        val index = Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            Js5Index.read(uncompressed)
        }
        indexes[archive] = index

        if (index.version != entry.version) {
            throw Exception("Index version invalid")
        }

        val groups = runBlocking {
            importer.importIndexAndGetMissingGroups(archive, index, buf)
        }
        for (group in groups) {
            request(archive, group)
        }
    }

    private fun request(archive: Int, group: Int) {
        pendingRequests += Js5Request.Group(false, archive, group)
    }

    private fun releaseGroups() {
        groups.forEach(CacheImporter.Group::release)
        groups.clear()
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
