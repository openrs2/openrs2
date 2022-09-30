package org.openrs2.archive.jav

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withContext
import org.openrs2.http.checkStatusCode
import java.io.BufferedReader
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

public data class JavConfig(
    public val config: Map<String, String>,
    public val params: Map<String, String>,
    public val messages: Map<String, String>
) {
    public companion object {
        public suspend fun download(client: HttpClient, url: String): JavConfig {
            val request = HttpRequest.newBuilder(URI(url))
                .GET()
                .timeout(Duration.ofSeconds(30))
                .build()

            val response = client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream()).await()
            response.checkStatusCode()

            return withContext(Dispatchers.IO) {
                response.body().bufferedReader().use { reader ->
                    read(reader)
                }
            }
        }

        public fun read(reader: BufferedReader): JavConfig {
            val config = mutableMapOf<String, String>()
            val params = mutableMapOf<String, String>()
            val messages = mutableMapOf<String, String>()

            reader.lineSequence().map(String::trim).forEach { line ->
                when {
                    line.startsWith("//") || line.startsWith("#") -> Unit
                    line.startsWith("msg=") -> {
                        val parts = line.substring("msg=".length).split("=", limit = 2)
                        if (parts.size == 2) {
                            messages[parts[0]] = parts[1]
                        }
                    }

                    line.startsWith("param=") -> {
                        val parts = line.substring("param=".length).split("=", limit = 2)
                        if (parts.size == 2) {
                            params[parts[0]] = parts[1]
                        }
                    }

                    else -> {
                        val parts = line.split("=", limit = 2)
                        if (parts.size == 2) {
                            config[parts[0]] = parts[1]
                        }
                    }
                }
            }

            return JavConfig(config, params, messages)
        }
    }
}
