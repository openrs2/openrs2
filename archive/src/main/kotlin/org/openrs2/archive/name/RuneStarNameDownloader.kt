package org.openrs2.archive.name

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.future.await
import org.openrs2.http.checkStatusCode
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import kotlin.streams.asSequence

@Singleton
public class RuneStarNameDownloader @Inject constructor(
    private val client: HttpClient
) : NameDownloader {
    override suspend fun download(): Sequence<String> {
        val names = mutableSetOf<String>()

        for (endpoint in NAMES_ENDPOINTS) {
            names += readTsv(endpoint, 4)
        }

        for (endpoint in INDIVIDUAL_NAMES_ENDPOINTS) {
            names += readTsv(endpoint, 0)
        }

        names += readTsv(LEANBOW_NAMES_ENDPOINT, 1)

        return names.asSequence()
    }

    private suspend fun readTsv(uri: URI, column: Int): Sequence<String> {
        val request = HttpRequest.newBuilder(uri)
            .GET()
            .timeout(Duration.ofSeconds(30))
            .build()

        val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofLines()).await()
        response.checkStatusCode()

        return response.body().map { line ->
            val columns = line.split('\t')
            if (column >= columns.size) {
                throw IOException("Column out of range")
            }
            columns[column]
        }.asSequence()
    }

    private companion object {
        private val NAMES_ENDPOINTS = listOf(
            URI("https://raw.githubusercontent.com/Joshua-F/cache-names/master/names.tsv"),
            URI("https://raw.githubusercontent.com/Pazaz/RT4-Data/main/names.tsv"),
            URI("https://raw.githubusercontent.com/Pazaz/RT4-Data/main/osrs.tsv"),
            URI("https://raw.githubusercontent.com/Pazaz/RT4-Data/main/walied.tsv"),
            URI("https://raw.githubusercontent.com/RuneStar/cache-names/master/names.tsv"),
        )
        private val INDIVIDUAL_NAMES_ENDPOINTS = listOf(
            URI("https://raw.githubusercontent.com/Joshua-F/cache-names/master/individual-names.tsv"),
            URI("https://raw.githubusercontent.com/Pazaz/RT4-Data/main/walied.individual.components.tsv"),
            URI("https://raw.githubusercontent.com/Pazaz/RT4-Data/main/walied.individual.tsv"),
            URI("https://raw.githubusercontent.com/RuneStar/cache-names/master/individual-names.tsv"),
        )
        private val LEANBOW_NAMES_ENDPOINT = URI("https://raw.githubusercontent.com/Pazaz/RT4-Data/main/leanbow.tsv")
    }
}
