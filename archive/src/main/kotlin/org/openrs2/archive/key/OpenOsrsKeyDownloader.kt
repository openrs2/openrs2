package org.openrs2.archive.key

import java.net.http.HttpClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class OpenOsrsKeyDownloader @Inject constructor(
    client: HttpClient,
    jsonKeyReader: JsonKeyReader
) : JsonKeyDownloader(client, jsonKeyReader) {
    override suspend fun getMissingUrls(seenUrls: Set<String>): Set<String> {
        return setOf(ENDPOINT)
    }

    private companion object {
        private const val ENDPOINT = "https://xtea.openosrs.dev/get"
    }
}
