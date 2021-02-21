package org.openrs2.http

import com.google.common.net.HttpHeaders
import com.google.common.net.MediaType
import java.io.IOException
import java.net.http.HttpResponse
import java.nio.charset.Charset

private val DEFAULT_EXPECTED_STATUS_CODES = setOf(200)

public fun <T> HttpResponse<T>.checkStatusCode(expectedStatusCodes: Set<Int> = DEFAULT_EXPECTED_STATUS_CODES) {
    val status = statusCode()
    if (status !in expectedStatusCodes) {
        throw IOException("Unexpected status code: $status")
    }
}

public val <T> HttpResponse<T>.contentType: MediaType?
    get() = headers().firstValue(HttpHeaders.CONTENT_TYPE).map(MediaType::parse).orElse(null)

public val <T> HttpResponse<T>.charset: Charset?
    get() {
        val contentType = contentType ?: return null
        return contentType.charset().orNull()
    }
