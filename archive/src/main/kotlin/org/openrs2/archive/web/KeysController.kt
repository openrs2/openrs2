package org.openrs2.archive.web

import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import org.openrs2.archive.key.KeyImporter
import org.openrs2.crypto.XteaKey
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class KeysController @Inject constructor(
    private val importer: KeyImporter
) {
    public suspend fun create(call: ApplicationCall) {
        val k0 = call.request.queryParameters["key1"]?.toIntOrNull()
        val k1 = call.request.queryParameters["key2"]?.toIntOrNull()
        val k2 = call.request.queryParameters["key3"]?.toIntOrNull()
        val k3 = call.request.queryParameters["key4"]?.toIntOrNull()

        if (k0 == null || k1 == null || k2 == null || k3 == null) {
            call.respond(HttpStatusCode.BadRequest)
            return
        }

        importer.import(listOf(XteaKey(k0, k1, k2, k3)))
        call.respond(HttpStatusCode.NoContent)
    }
}
