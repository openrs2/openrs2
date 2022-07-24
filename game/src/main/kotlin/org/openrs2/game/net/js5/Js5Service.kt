package org.openrs2.game.net.js5

import com.google.common.util.concurrent.AbstractExecutionThreadService
import io.netty.buffer.ByteBufAllocator
import org.openrs2.buffer.use
import org.openrs2.cache.Js5Archive
import org.openrs2.cache.Js5Compression
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.Js5MasterIndex
import org.openrs2.cache.Store
import org.openrs2.cache.VersionTrailer
import org.openrs2.protocol.js5.downstream.Js5Response
import org.openrs2.protocol.js5.upstream.Js5Request
import org.openrs2.util.collect.UniqueQueue
import java.io.FileNotFoundException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class Js5Service @Inject constructor(
    private val store: Store,
    private val masterIndex: Js5MasterIndex,
    private val alloc: ByteBufAllocator
) : AbstractExecutionThreadService() {
    private val lock = Object()
    private val clients = UniqueQueue<Js5Client>()

    override fun run() {
        while (true) {
            var client: Js5Client
            var request: Js5Request.Group

            synchronized(lock) {
                while (true) {
                    if (!isRunning) {
                        return
                    }

                    val next = clients.removeFirstOrNull()
                    if (next == null) {
                        lock.wait()
                        continue
                    }

                    client = next
                    request = client.pop() ?: continue
                    break
                }
            }

            serve(client, request)
        }
    }

    private fun serve(client: Js5Client, request: Js5Request.Group) {
        val ctx = client.ctx
        if (!ctx.channel().isActive) {
            return
        }

        val buf = if (request.archive == Js5Archive.ARCHIVESET && request.group == Js5Archive.ARCHIVESET) {
            alloc.buffer().use { uncompressed ->
                masterIndex.write(uncompressed)

                Js5Compression.compress(uncompressed, Js5CompressionType.UNCOMPRESSED).use { compressed ->
                    compressed.retain()
                }
            }
        } else {
            try {
                store.read(request.archive, request.group).use { buf ->
                    if (request.archive != Js5Archive.ARCHIVESET) {
                        VersionTrailer.strip(buf)
                    }

                    buf.retain()
                }
            } catch (ex: FileNotFoundException) {
                ctx.close()
                return
            }
        }

        val response = Js5Response(request.prefetch, request.archive, request.group, buf)
        ctx.writeAndFlush(response, ctx.voidPromise())

        synchronized(lock) {
            if (client.isReady()) {
                clients.add(client)
            }

            if (client.isNotFull()) {
                ctx.read()
            }
        }
    }

    public fun push(client: Js5Client, request: Js5Request.Group) {
        synchronized(lock) {
            client.push(request)

            if (client.isReady()) {
                clients.add(client)
                lock.notifyAll()
            }

            if (client.isNotFull()) {
                client.ctx.read()
            }
        }
    }

    public fun readIfNotFull(client: Js5Client) {
        synchronized(lock) {
            if (client.isNotFull()) {
                client.ctx.read()
            }
        }
    }

    public fun notifyIfNotEmpty(client: Js5Client) {
        synchronized(lock) {
            if (client.isNotEmpty()) {
                lock.notifyAll()
            }
        }
    }

    override fun triggerShutdown() {
        synchronized(lock) {
            lock.notifyAll()
        }
    }
}
