package org.openrs2.game.net.http

import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpResponse
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion

public object Http {
    public const val MAX_CONTENT_LENGTH: Int = 65536
    public const val TEXT_X_CROSS_DOMAIN_POLICY: String = "text/x-cross-domain-policy"

    public fun createResponse(status: HttpResponseStatus): HttpResponse {
        val response = DefaultHttpResponse(HttpVersion.HTTP_1_1, status)
        response.headers().add(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE)
        response.headers().add(HttpHeaderNames.SERVER, "OpenRS2")
        return response
    }
}
