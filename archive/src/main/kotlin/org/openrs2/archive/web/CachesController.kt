package org.openrs2.archive.web

import io.ktor.application.ApplicationCall
import io.ktor.http.ContentType
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

    public suspend fun export(call: ApplicationCall) {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        call.response.header("Content-Disposition", "attachment; filename=\"cache.zip\"")
        call.respondOutputStream(contentType = ContentType.Application.Zip) {
            DiskStoreZipWriter(ZipOutputStream(this), alloc = alloc).use { store ->
                exporter.export(id, store)
            }
        }
    }
}
