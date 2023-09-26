package org.openrs2.archive.web

import io.ktor.http.ContentDisposition
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.thymeleaf.ThymeleafContent
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.archive.client.ClientExporter

@Singleton
public class ClientsController @Inject constructor(
    private val exporter: ClientExporter
) {
    public suspend fun index(call: ApplicationCall) {
        val artifacts = exporter.list()

        call.respond(
            ThymeleafContent(
                "clients/index.html", mapOf(
                    "artifacts" to artifacts
                )
            )
        )
    }

    public suspend fun show(call: ApplicationCall) {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        val artifact = exporter.get(id)
        if (artifact == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        call.respond(
            ThymeleafContent(
                "clients/show.html", mapOf(
                    "artifact" to artifact
                )
            )
        )
    }

    public suspend fun export(call: ApplicationCall) {
        val id = call.parameters["id"]?.toLongOrNull()
        if (id == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        val artifact = exporter.export(id)
        if (artifact == null) {
            call.respond(HttpStatusCode.NotFound)
            return
        }

        call.response.header(
            HttpHeaders.ContentLength,
            artifact.summary.size.toString()
        )

        call.response.header(
            HttpHeaders.ContentDisposition,
            ContentDisposition.Attachment
                .withParameter(ContentDisposition.Parameters.FileName, artifact.summary.name)
                .toString()
        )

        call.respondOutputStream(artifact.summary.format.getContentType(artifact.summary.os)) {
            artifact.content().readBytes(this, artifact.summary.size)
        }
    }
}
