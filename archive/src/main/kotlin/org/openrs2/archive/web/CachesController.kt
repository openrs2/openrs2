package org.openrs2.archive.web

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentDisposition
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.response.header
import io.ktor.response.respond
import io.ktor.response.respondOutputStream
import io.ktor.thymeleaf.ThymeleafContent
import io.netty.buffer.ByteBufAllocator
import org.openrs2.archive.cache.CacheExporter
import org.openrs2.cache.DiskStoreZipWriter
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class CachesController @Inject constructor(
    private val exporter: CacheExporter,
    private val alloc: ByteBufAllocator
) {
    public suspend fun index(call: ApplicationCall) {
        val caches = exporter.list()
        call.respond(ThymeleafContent("caches/index.html", mapOf("caches" to caches)))
    }

    public suspend fun show(call: ApplicationCall) {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        val cache = exporter.get(id)
        if (cache == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        call.respond(ThymeleafContent("caches/show.html", mapOf("cache" to cache)))
    }

    public suspend fun export(call: ApplicationCall) {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment
                .withParameter(ContentDisposition.Parameters.FileName, "cache.zip")
                .toString()
        )

        call.respondOutputStream(contentType = ContentType.Application.Zip) {
            DiskStoreZipWriter(ZipOutputStream(this), alloc = alloc).use { store ->
                exporter.export(id, store)
            }
        }
    }

    public suspend fun exportKeys(call: ApplicationCall) {
        val id = call.parameters["id"]?.toIntOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        call.respond(exporter.exportKeys(id))
    }
}
