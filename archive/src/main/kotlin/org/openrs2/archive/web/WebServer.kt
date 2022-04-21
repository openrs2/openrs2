package org.openrs2.archive.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.ContentType
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.thymeleaf.Thymeleaf
import io.ktor.thymeleaf.ThymeleafContent
import io.ktor.webjars.Webjars
import org.openrs2.json.Json
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class WebServer @Inject constructor(
    private val cachesController: CachesController,
    private val keysController: KeysController,
    @Json private val mapper: ObjectMapper
) {
    public fun start(address: String, port: Int) {
        embeddedServer(CIO, host = address, port = port) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }

            install(Thymeleaf) {
                addDialect(ByteUnitsDialect)
                addDialect(Java8TimeDialect())

                setTemplateResolver(ClassLoaderTemplateResolver().apply {
                    prefix = "/org/openrs2/archive/templates/"
                    templateMode = TemplateMode.HTML
                })
            }

            install(XForwardedHeaderSupport)
            install(Webjars)

            routing {
                get("/") { call.respond(ThymeleafContent("index.html", emptyMap())) }
                get("/caches") { cachesController.index(call) }
                get("/caches.json") { cachesController.indexJson(call) }
                get("/caches/{scope}/{id}") { cachesController.show(call) }
                get("/caches/{scope}/{id}/disk.zip") { cachesController.exportDisk(call) }
                get("/caches/{scope}/{id}/flat-file.tar.gz") { cachesController.exportFlatFile(call) }
                get("/caches/{scope}/{id}/keys.json") { cachesController.exportKeysJson(call) }
                get("/caches/{scope}/{id}/keys.zip") { cachesController.exportKeysZip(call) }
                get("/caches/{scope}/{id}/map.png") { cachesController.renderMap(call) }
                get("/keys") { keysController.index(call) }
                post("/keys") { keysController.import(call) }
                get("/keys/all.json") { keysController.exportAll(call) }
                get("/keys/valid.json") { keysController.exportValid(call) }
                static("/static") { resources("/org/openrs2/archive/static") }

                // compatibility redirects
                get("/caches/{id}") { redirect(call, permanent = true, "/caches/runescape/{id}") }
                get("/caches/{id}.json") { redirect(call, permanent = true, "/caches/runescape/{id}/keys.json") }
                get("/caches/{id}.zip") { redirect(call, permanent = true, "/caches/runescape/{id}/disk.zip") }
                get("/caches/{id}/disk.zip") { redirect(call, permanent = true, "/caches/runescape/{id}/disk.zip") }
                get("/caches/{id}/flat-file.tar.gz") {
                    redirect(call, permanent = true, "/caches/runescape/{id}/flat-file.tar.gz")
                }
                get("/caches/{id}/keys.json") { redirect(call, permanent = true, "/caches/runescape/{id}/keys.json") }
                get("/caches/{id}/keys.zip") { redirect(call, permanent = true, "/caches/runescape/{id}/keys.zip") }
                get("/caches/{id}/map.png") { redirect(call, permanent = true, "/caches/runescape/{id}/map.png") }
            }
        }.start(wait = true)
    }

    private suspend fun redirect(call: ApplicationCall, permanent: Boolean, path: String) {
        val destination = path.replace(PARAMETER) { match ->
            val (name) = match.destructured
            call.parameters[name] ?: throw IllegalArgumentException()
        }

        call.respondRedirect(destination, permanent)
    }

    private companion object {
        private val PARAMETER = Regex("\\{([^}]*)}")
    }
}
