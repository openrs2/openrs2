package org.openrs2.archive.key

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.openrs2.http.charset
import org.openrs2.http.checkStatusCode
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class PolarKeyDownloader @Inject constructor(
    private val client: HttpClient,
    jsonKeyReader: JsonKeyReader
) : JsonKeyDownloader(client, jsonKeyReader) {
    override suspend fun getMissingUrls(seenUrls: Set<String>): Set<String> {
        val request = HttpRequest.newBuilder(ENDPOINT)
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).await()
        response.checkStatusCode()

        val document = withContext(Dispatchers.IO) {
            Jsoup.parse(response.body(), response.charset?.name(), ENDPOINT.toString())
        }

        val urls = mutableSetOf<String>()

        for (element in document.select("a")) {
            val url = element.absUrl("href")
            if (url.endsWith(".json") && url !in seenUrls) {
                urls += url
            }
        }

        return urls
    }

    private companion object {
        private val ENDPOINT = URI("https://archive.runestats.com/osrs/xtea/")
    }
}
