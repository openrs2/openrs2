package org.openrs2.archive.web

import io.ktor.application.ApplicationCall
import io.ktor.response.respond
import io.ktor.thymeleaf.ThymeleafContent
import org.openrs2.archive.key.KeyExporter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeysController @Inject constructor(
    private val exporter: KeyExporter
) {
    public suspend fun index(call: ApplicationCall) {
        val (all, valid) = exporter.count()
        call.respond(ThymeleafContent("keys/index.html", mapOf("all" to all, "valid" to valid)))
    }

    public suspend fun exportAll(call: ApplicationCall) {
        call.respond(exporter.exportAll())
    }

    public suspend fun exportValid(call: ApplicationCall) {
        call.respond(exporter.exportValid())
    }
}
