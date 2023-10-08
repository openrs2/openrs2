package org.openrs2.archive.cache

import com.github.michaelbull.logging.InlineLogger
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelException
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPipeline
import io.netty.channel.SimpleChannelInboundHandler
import kotlinx.coroutines.runBlocking
import org.openrs2.buffer.crc32
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5Index
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.MasterIndexFormat
import java.io.IOException
import java.nio.channels.ClosedChannelException
import java.time.Instant
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@ChannelHandler.Sharable
public abstract class Js5ChannelHandler(
    private val bootstrap: Bootstrap,
    private val scopeId: Int,
    private val gameId: Int,
    private val hostname: String,
    private val port: Int,
    protected var buildMajor: Int,
    protected var buildMinor: Int?,
    private val lastMasterIndexId: Int?,
    private val continuation: Continuation<Unit>,
    private val importer: CacheImporter,
    private val masterIndexFormat: MasterIndexFormat,
    private val maxInFlightRequests: Int,
    private val maxBuildAttempts: Int = 10,
    private val maxReconnectionAttempts: Int = 1
) : SimpleChannelInboundHandler<Any>(Object::class.java) {
    protected data class InFlightRequest(val prefetch: Boolean, val archive: Int, val group: Int)
    protected data class PendingRequest(
        val prefetch: Boolean,
        val archive: Int,
        val group: Int,
        val version: Int,
        val checksum: Int
    )

    private enum class State {
        CONNECTING,
        CLIENT_OUT_OF_DATE,
        CONNECTED,
        RESUMING_CONTINUATION
    }

    private var state = State.CONNECTING
    private var buildAttempts = 0
    private var reconnectionAttempts = 0
    private val inFlightRequests = mutableSetOf<InFlightRequest>()
    private val pendingRequests = ArrayDeque<PendingRequest>()
    private var masterIndexId: Int = 0
    private var sourceId: Int = 0
    private var masterIndex: Js5MasterIndex? = null
    private lateinit var indexes: Array<Js5Index?>
    private val groups = mutableListOf<CacheImporter.Group>()

    protected abstract fun createInitMessage(): Any
    protected abstract fun createRequestMessage(prefetch: Boolean, archive: Int, group: Int): Any
    protected abstract fun createConnectedMessage(): Any?
    protected abstract fun configurePipeline(pipeline: ChannelPipeline)
    protected abstract fun incrementVersion()

    override fun channelActive(ctx: ChannelHandlerContext) {
        assert(state == State.CONNECTING)

        ctx.writeAndFlush(createInitMessage(), ctx.voidPromise())
        ctx.read()
    }

    override fun channelReadComplete(ctx: ChannelHandlerContext) {
        /*
         * Wait for us to receive the OK message before we send JS5 requests,
         * as the RS3 JS5 server ignores any JS5 requests sent before the OK
         * message is received.
         */
        if (state != State.CONNECTED) {
            return
        }

        var flush = false

        while (inFlightRequests.size < maxInFlightRequests) {
            val request = pendingRequests.removeFirstOrNull() ?: break
            inFlightRequests += InFlightRequest(request.prefetch, request.archive, request.group)

            logger.info { "Requesting archive ${request.archive} group ${request.group}" }
            ctx.write(createRequestMessage(request.prefetch, request.archive, request.group), ctx.voidPromise())

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
            state = State.CONNECTING
            bootstrap.connect(hostname, port)
        } else if (state != State.RESUMING_CONTINUATION) {
            if (isComplete()) {
                throw Exception("Connection closed unexpectedly")
            } else if (++reconnectionAttempts > maxReconnectionAttempts) {
                throw Exception("Connection closed unexpectedly after maximum number of reconnection attempts")
            }

            // move in-flight requests back to the pending queue
            for (request in inFlightRequests) {
                val prefetch = request.prefetch
                val archive = request.archive
                val group = request.group

                pendingRequests += if (archive == Js5Archive.ARCHIVESET && group == Js5Archive.ARCHIVESET) {
                    PendingRequest(prefetch, archive, group, 0, 0)
                } else if (archive == Js5Archive.ARCHIVESET) {
                    val entry = masterIndex!!.entries[group]
                    val version = entry.version
                    val checksum = entry.checksum

                    PendingRequest(prefetch, archive, group, version, checksum)
                } else {
                    val entry = indexes[archive]!![group]!!
                    val version = entry.version
                    val checksum = entry.checksum

                    PendingRequest(prefetch, archive, group, version, checksum)
                }
            }

            inFlightRequests.clear()

            // re-connect
            state = State.CONNECTING
            bootstrap.connect(hostname, port)
        }
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        releaseGroups()

        if (state == State.RESUMING_CONTINUATION) {
            logger.warn(cause) { "Swallowing exception as continuation has already resumed" }
        } else if (cause !is ChannelException && cause !is IOException) {
            /*
             * We skip continuation resumption if there's an I/O error or
             * timeout - this allows channelInactive() to attempt to reconnect
             * if we haven't used too many reconnection attempts.
             */
            state = State.RESUMING_CONTINUATION
            continuation.resumeWithException(cause)
        }

        if (cause !is ClosedChannelException) {
            ctx.close()
        }
    }

    protected fun handleOk(ctx: ChannelHandlerContext) {
        assert(state == State.CONNECTING)

        configurePipeline(ctx.pipeline())

        val msg = createConnectedMessage()
        if (msg != null) {
            ctx.write(msg, ctx.voidPromise())
        }

        state = State.CONNECTED

        if (masterIndex == null && pendingRequests.isEmpty()) {
            request(ctx, Js5Archive.ARCHIVESET, Js5Archive.ARCHIVESET, 0, 0)
        }
    }

    protected fun handleClientOutOfDate(ctx: ChannelHandlerContext) {
        assert(state == State.CONNECTING)

        if (++buildAttempts > maxBuildAttempts) {
            throw Exception("Failed to identify current version")
        }

        state = State.CLIENT_OUT_OF_DATE
        incrementVersion()

        ctx.close()
    }

    protected fun handleResponse(
        ctx: ChannelHandlerContext,
        prefetch: Boolean,
        archive: Int,
        group: Int,
        data: ByteBuf
    ) {
        val request = InFlightRequest(prefetch, archive, group)

        val removed = inFlightRequests.remove(request)
        if (!removed) {
            val type = if (prefetch) {
                "prefetch"
            } else {
                "urgent"
            }
            throw Exception("Received response for $type request (archive $archive group $group) not in-flight")
        }

        processResponse(ctx, archive, group, data)
    }

    protected fun processResponse(ctx: ChannelHandlerContext, archive: Int, group: Int, data: ByteBuf) {
        if (archive == Js5Archive.ARCHIVESET && group == Js5Archive.ARCHIVESET) {
            processMasterIndex(ctx, data)
        } else if (archive == Js5Archive.ARCHIVESET) {
            processIndex(ctx, group, data)
        } else {
            processGroup(archive, group, data)
        }

        val complete = isComplete()

        if (groups.size >= CacheImporter.BATCH_SIZE || complete) {
            runBlocking {
                importer.importGroups(scopeId, sourceId, groups)
            }

            releaseGroups()
        }

        if (complete) {
            runBlocking {
                importer.setLastMasterIndexId(gameId, masterIndexId)
            }

            state = State.RESUMING_CONTINUATION
            continuation.resume(Unit)

            ctx.close()
        } else {
            /*
             * Reset the number of reconnection attempts as we are making
             * progress.
             */
            reconnectionAttempts = 0
        }
    }

    protected open fun isComplete(): Boolean {
        return pendingRequests.isEmpty() && inFlightRequests.isEmpty()
    }

    private fun processMasterIndex(ctx: ChannelHandlerContext, buf: ByteBuf) {
        Js5Compression.uncompress(buf.slice()).use { uncompressed ->
            masterIndex = Js5MasterIndex.readUnverified(uncompressed.slice(), masterIndexFormat)

            val (masterIndexId, sourceId, rawIndexes) = runBlocking {
                importer.importMasterIndexAndGetIndexes(
                    masterIndex!!,
                    buf,
                    uncompressed,
                    gameId,
                    scopeId,
                    buildMajor,
                    buildMinor,
                    lastMasterIndexId,
                    timestamp = Instant.now()
                )
            }

            this.masterIndexId = masterIndexId
            this.sourceId = sourceId

            try {
                indexes = arrayOfNulls(rawIndexes.size)

                for ((archive, index) in rawIndexes.withIndex()) {
                    val entry = masterIndex!!.entries[archive]
                    if (entry.version == 0 && entry.checksum == 0) {
                        continue
                    }

                    if (index != null) {
                        processIndex(ctx, archive, index)
                    } else {
                        request(ctx, Js5Archive.ARCHIVESET, archive, entry.version, entry.checksum)
                    }
                }
            } finally {
                rawIndexes.filterNotNull().forEach(ByteBuf::release)
            }
        }
    }

    private fun processIndex(ctx: ChannelHandlerContext, archive: Int, buf: ByteBuf) {
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
                importer.importIndexAndGetMissingGroups(
                    scopeId,
                    sourceId,
                    archive,
                    index,
                    buf,
                    uncompressed,
                    lastMasterIndexId
                )
            }
            for (group in groups) {
                val groupEntry = index[group]!!
                request(ctx, archive, group, groupEntry.version, groupEntry.checksum)
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

    protected open fun request(ctx: ChannelHandlerContext, archive: Int, group: Int, version: Int, checksum: Int) {
        pendingRequests += PendingRequest(false, archive, group, version, checksum)
    }

    private fun releaseGroups() {
        groups.forEach(CacheImporter.Group::release)
        groups.clear()
    }

    private companion object {
        private val logger = InlineLogger()
    }
}
