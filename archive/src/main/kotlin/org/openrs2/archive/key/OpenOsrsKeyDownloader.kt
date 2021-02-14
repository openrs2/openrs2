package org.openrs2.archive.key

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.openrs2.crypto.XteaKey
import org.openrs2.http.checkStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class OpenOsrsKeyDownloader @Inject constructor(
    private val client: HttpClient,
    private val jsonKeyReader: JsonKeyReader
) : KeyDownloader {
    override suspend fun download(): Sequence<XteaKey> {
        val request = HttpRequest.newBuilder(ENDPOINT)
            .GET()
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).await()
        response.checkStatusCode()

        return withContext(Dispatchers.IO) {
            response.body().use { input ->
                jsonKeyReader.read(input)
            }
        }
    }

    private companion object {
        private val ENDPOINT = URI("https://xtea.openosrs.dev/get")
    }
}
