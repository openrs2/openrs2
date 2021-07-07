package org.openrs2.archive.cache.nxt

import io.netty.buffer.ByteBuf
import kotlinx.coroutines.future.await
import org.openrs2.buffer.ByteBufBodyHandler
import org.openrs2.buffer.use
import org.openrs2.http.checkStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest

public class MusicStreamClient(
    private val client: HttpClient,
    private val byteBufBodyHandler: ByteBufBodyHandler,
    private val origin: String
) {
    public suspend fun request(archive: Int, group: Int, version: Int, checksum: Int, build: Int): ByteBuf {
        val uri = URI("$origin/ms?m=0&a=$archive&k=$build&g=$group&c=$checksum&v=$version")

        val request = HttpRequest.newBuilder(uri)
            .GET()
            .build()

        val response = client.sendAsync(request, byteBufBodyHandler).await()
        response.body().use { buf ->
            response.checkStatusCode()
            return buf.retain()
        }
    }
}
