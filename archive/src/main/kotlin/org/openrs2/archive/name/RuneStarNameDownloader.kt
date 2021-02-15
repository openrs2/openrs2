package org.openrs2.archive.name

import kotlinx.coroutines.future.await
import org.openrs2.http.checkStatusCode
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.streams.asSequence

@Singleton
public class RuneStarNameDownloader @Inject constructor(
    private val client: HttpClient
) : NameDownloader {
    override suspend fun download(): Sequence<String> {
        val names = readTsv(NAMES_ENDPOINT, 4)
        val individualNames = readTsv(INDIVIDUAL_NAMES_ENDPOINT, 0)
        return names + individualNames
    }

    private suspend fun readTsv(uri: URI, column: Int): Sequence<String> {
        val request = HttpRequest.newBuilder(uri)
            .GET()
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
        private val NAMES_ENDPOINT = URI("https://raw.githubusercontent.com/RuneStar/cache-names/master/names.tsv")
        private val INDIVIDUAL_NAMES_ENDPOINT =
            URI("https://raw.githubusercontent.com/RuneStar/cache-names/master/individual-names.tsv")
    }
}
