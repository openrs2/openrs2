package org.openrs2.http

import java.io.IOException
import java.net.http.HttpResponse

private val DEFAULT_EXPECTED_STATUS_CODES = setOf(200)

public fun <T> HttpResponse<T>.checkStatusCode(expectedStatusCodes: Set<Int> = DEFAULT_EXPECTED_STATUS_CODES) {
    val status = statusCode()
    if (status !in expectedStatusCodes) {
        throw IOException("Unexpected status code: $status")
    }
}
