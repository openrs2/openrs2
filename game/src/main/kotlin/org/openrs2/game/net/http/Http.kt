package org.openrs2.game.net.http

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.FileRegion
import io.netty.handler.codec.http.DefaultHttpResponse
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpHeaderValues
import io.netty.handler.codec.http.HttpMethod
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.HttpResponseStatus
import io.netty.handler.codec.http.HttpVersion
import io.netty.handler.codec.http.LastHttpContent
import io.netty.util.ReferenceCounted
import org.openrs2.buffer.copiedBuffer
import org.openrs2.buffer.use

public object Http {
    public const val MAX_CONTENT_LENGTH: Int = 65536

    public const val APPLICATION_JAVA_ARCHIVE: String = "application/java-archive"
    public const val TEXT_X_CROSS_DOMAIN_POLICY: String = "text/x-cross-domain-policy"

    private const val BANNER = "OpenRS2"

    private fun isKeepAlive(request: HttpRequest, version: HttpVersion): Boolean {
        val connection = request.headers().get(HttpHeaderNames.CONNECTION) ?: return version.isKeepAliveDefault
        return HttpHeaderValues.KEEP_ALIVE.contentEquals(connection)
    }

    private fun writeResponse(
        ctx: ChannelHandlerContext,
        request: HttpRequest,
        status: HttpResponseStatus,
        content: ReferenceCounted,
        contentType: CharSequence,
        contentLength: Long
    ) {
        val version = if (request.protocolVersion() == HttpVersion.HTTP_1_0) {
            HttpVersion.HTTP_1_0
        } else {
            HttpVersion.HTTP_1_1
        }

        val keepAlive = isKeepAlive(request, version)

        val response = DefaultHttpResponse(version, status)
        response.headers().add(HttpHeaderNames.CONNECTION, if (keepAlive) {
            HttpHeaderValues.KEEP_ALIVE
        } else {
            HttpHeaderValues.CLOSE
        })
        response.headers().add(HttpHeaderNames.SERVER, BANNER)
        response.headers().add(HttpHeaderNames.CONTENT_TYPE, contentType)
        response.headers().add(HttpHeaderNames.CONTENT_LENGTH, contentLength)

        ctx.write(response, ctx.voidPromise())

        if (request.method() != HttpMethod.HEAD) {
            ctx.write(content.retain(), ctx.voidPromise())
        }

        writeLastChunk(ctx, keepAlive)
    }

    private fun writeLastChunk(ctx: ChannelHandlerContext, keepAlive: Boolean) {
        if (keepAlive) {
            ctx.write(LastHttpContent.EMPTY_LAST_CONTENT, ctx.voidPromise())
        } else {
            ctx.write(LastHttpContent.EMPTY_LAST_CONTENT).addListener(ChannelFutureListener.CLOSE)
        }
    }

    public fun writeResponse(
        ctx: ChannelHandlerContext,
        request: HttpRequest,
        content: ByteBuf,
        contentType: CharSequence
    ) {
        writeResponse(ctx, request, HttpResponseStatus.OK, content, contentType, content.readableBytes().toLong())
    }

    public fun writeResponse(
        ctx: ChannelHandlerContext,
        request: HttpRequest,
        content: FileRegion,
        contentType: CharSequence
    ) {
        writeResponse(ctx, request, HttpResponseStatus.OK, content, contentType, content.count())
    }

    public fun writeResponse(ctx: ChannelHandlerContext, request: HttpRequest, status: HttpResponseStatus) {
        copiedBuffer(
            """
            <!DOCTYPE html>
            <html lang="en">
                <head>
                    <meta charset="UTF-8" />
                    <title>$status</title>
                </head>
                <body>
                    <center>
                        <h1>$status</h1>
                    </center>
                    <hr />
                    <center>$BANNER</center>
                </body>
            </html>
        """.trimIndent().plus("\n"), Charsets.UTF_8
        ).use { buf ->
            writeResponse(ctx, request, status, buf, HttpHeaderValues.TEXT_HTML, buf.readableBytes().toLong())
        }
    }
}
