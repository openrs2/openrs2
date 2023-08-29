package org.openrs2.archive.key

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.jdom2.input.SAXBuilder
import org.openrs2.http.checkStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Singleton
public class RuneLiteKeyDownloader @Inject constructor(
    private val client: HttpClient,
    jsonKeyReader: JsonKeyReader
) : JsonKeyDownloader(KeySource.RUNELITE, client, jsonKeyReader) {
    override suspend fun getMissingUrls(seenUrls: Set<String>): Set<String> {
        val version = getVersion()
        return setOf(getXteaEndpoint(version))
    }

    private suspend fun getVersion(): String {
        val request = HttpRequest.newBuilder(VERSION_ENDPOINT)
            .GET()
            .timeout(Duration.ofSeconds(30))
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

        private fun getXteaEndpoint(version: String): String {
            return "https://api.runelite.net/runelite-$version/xtea"
        }
    }
}
