package org.openrs2.archive.key

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.jdom2.input.SAXBuilder
import org.openrs2.crypto.XteaKey
import org.openrs2.http.checkStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class RuneLiteKeyDownloader @Inject constructor(
    private val client: HttpClient,
    private val jsonKeyReader: JsonKeyReader
) : KeyDownloader {
    override suspend fun download(): Sequence<XteaKey> {
        val version = getVersion()

        val request = HttpRequest.newBuilder(getXteaEndpoint(version))
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

    private suspend fun getVersion(): String {
        val request = HttpRequest.newBuilder(VERSION_ENDPOINT)
            .GET()
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).await()
        response.checkStatusCode()

        val document = withContext(Dispatchers.IO) {
            response.body().use { input ->
                SAXBuilder().build(input)
            }
        }

        return document.rootElement
            .getChild("versioning")
            .getChild("release")
            .textTrim
    }

    private companion object {
        private val VERSION_ENDPOINT = URI("https://repo.runelite.net/net/runelite/runelite-parent/maven-metadata.xml")

        private fun getXteaEndpoint(version: String): URI {
            return URI("https://api.runelite.net/runelite-$version/xtea")
        }
    }
}
