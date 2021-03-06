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
    private val lastMasterIndexId: Int?,
    private val continuation: Continuation<Unit>,
    private val importer: CacheImporter,
    private val masterIndexFormat: MasterIndexFormat = MasterIndexFormat.VERSIONED,
    private val maxInFlightRequests: Int = 200,
    maxBuildAttempts: Int = 10
) : SimpleChannelInboundHandler<Any>(Object::class.java) {
    private enum class State {
        ACTIVE,
        CLIENT_OUT_OF_DATE,
        RESUMING_CONTINUATION
    }

    private var state = State.ACTIVE
    private val maxBuild = build + maxBuildAttempts
    private val inFlightRequests = mutableSetOf<Js5Request.Group>()
    private val pendingRequests = ArrayDeque<Js5Request.Group>()
    private var masterIndexId: Int = 0
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

    override fun channelInactive(ctx: ChannelHandlerContext) {
        if (state == State.CLIENT_OUT_OF_DATE) {
            state = State.ACTIVE
            bootstrap.connect(hostname, port)
        } else if (state != State.RESUMING_CONTINUATION) {
            throw Exception("Connection closed unexpectedly")
        }
    }

    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        releaseGroups()

        state = State.RESUMING_CONTINUATION
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

        state = State.CLIENT_OUT_OF_DATE
        ctx.close()
    }

    private fun handleResponse(ctx: ChannelHandlerContext, response: Js5Response) {
        val request = Js5Request.Group(response.prefetch, response.archive, response.group)

        val removed = inFlightRequests.remove(request)
        if (!removed) {
            val type = if (response.prefetch) {
                "prefetch"
            } else {
                "urgent"
            }
            val archive = response.archive
            val group = response.group
            throw Exception("Received response for $type request (archive $archive group $group) not in-flight")
        }

        if (response.archive == Js5Archive.ARCHIVESET && response.group == Js5Archive.ARCHIVESET) {
            processMasterIndex(response.data)
        } else if (response.archive == Js5Archive.ARCHIVESET) {
            processIndex(response.group, response.data)
        } else {
            processGroup(response.archive, response.group, response.data)
        }

        val complete = pendingRequests.isEmpty() && inFlightRequests.isEmpty()

        if (groups.size >= CacheImporter.BATCH_SIZE || complete) {
            runBlocking {
                importer.importGroups(groups)
            }

            releaseGroups()
        }

        if (complete) {
            runBlocking {
                importer.setLastMasterIndexId(gameId, masterIndexId)
            }

            state = State.RESUMING_CONTINUATION
            ctx.close()
            continuation.resume(Unit)
        }
    }

    private fun processMasterIndex(buf: ByteBuf) {
        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            masterIndex = Js5MasterIndex.read(uncompressed.slice(), masterIndexFormat)

            val (id, rawIndexes) = runBlocking {
                importer.importMasterIndexAndGetIndexes(
                    masterIndex!!,
                    buf,
                    uncompressed,
                    gameId,
                    build,
                    lastMasterIndexId,
                    timestamp = Instant.now(),
                    name = "Original"
                )
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

            masterIndexId = id
        }
    }

    private fun processIndex(archive: Int, buf: ByteBuf) {
        val checksum = buf.crc32()
        val entry = masterIndex!!.entries[archive]
        if (checksum != entry.checksum) {
            throw Exception("Index $archive checksum invalid (expected ${entry.checksum}, actual $checksum)")
        }

        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            val index = Js5Index.read(uncompressed.slice())
            indexes[archive] = index

            if (index.version != entry.version) {
                throw Exception("Index $archive version invalid (expected ${entry.version}, actual ${index.version})")
            }

            val groups = runBlocking {
                importer.importIndexAndGetMissingGroups(archive, index, buf, uncompressed, lastMasterIndexId)
            }
            for (group in groups) {
                request(archive, group)
            }
        }
    }

    private fun processGroup(archive: Int, group: Int, buf: ByteBuf) {
        val checksum = buf.crc32()
        val entry = indexes[archive]!![group]!!
        if (checksum != entry.checksum) {
            val expected = entry.checksum
            throw Exception("Archive $archive group $group checksum invalid (expected $expected, actual $checksum)")
        }

        val uncompressed = Js5Compression.uncompressUnlessEncrypted(buf.slice())
        groups += CacheImporter.Group(
            archive,
            group,
            buf.retain(),
            uncompressed,
            entry.version,
            versionTruncated = false
        )
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
