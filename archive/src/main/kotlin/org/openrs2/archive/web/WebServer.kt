package org.openrs2.archive.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.features.XForwardedHeaderSupport
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.jackson.JacksonConverter
import io.ktor.response.respond
import io.ktor.response.respondRedirect
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
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
        embeddedServer(Netty, host = address, port = port) {
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(mapper))
            }

            install(Thymeleaf) {
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
                get("/caches/{id}") { cachesController.show(call) }
                get("/caches/{id}.zip") { cachesController.export(call) }
                get("/caches/{id}.json") {
                    val id = call.parameters["id"]
                    if (id == null) {
                        call.respond(HttpStatusCode.NotFound)
                        return@get
                    }

                    call.respondRedirect(permanent = true) {
                        path("caches", id, "keys.json")
                    }
                }
                get("/caches/{id}/keys.json") { cachesController.exportKeysJson(call) }
                get("/caches/{id}/keys.zip") { cachesController.exportKeysZip(call) }
                get("/caches/{id}/map.png") { cachesController.renderMap(call) }
                get("/keys") { keysController.index(call) }
                get("/keys/all.json") { keysController.exportAll(call) }
                get("/keys/valid.json") { keysController.exportValid(call) }
                static("/static") { resources("/org/openrs2/archive/static") }
            }
        }.start(wait = true)
    }
}
