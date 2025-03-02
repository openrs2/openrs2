package org.openrs2.archive.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.jetty.jakarta.Jetty
import io.ktor.server.plugins.autohead.AutoHeadResponse
import io.ktor.server.plugins.cachingheaders.CachingHeaders
import io.ktor.server.plugins.conditionalheaders.ConditionalHeaders
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.defaultheaders.DefaultHeaders
import io.ktor.server.plugins.forwardedheaders.XForwardedHeaders
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.server.thymeleaf.Thymeleaf
import io.ktor.server.thymeleaf.ThymeleafContent
import io.ktor.server.webjars.Webjars
import jakarta.inject.Inject
import jakarta.inject.Singleton
import org.openrs2.json.Json
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import kotlin.time.Duration.Companion.minutes

@Singleton
public class WebServer @Inject constructor(
    private val cachesController: CachesController,
    private val clientsController: ClientsController,
    private val keysController: KeysController,
    @Json private val mapper: ObjectMapper
) {
    public fun start(address: String, port: Int) {
        embeddedServer(Jetty, configure = {
            connector {
                host = address
                this.port = port
            }

            idleTimeout = 5.minutes
        }) {
            install(AutoHeadResponse)
            install(CachingHeaders)
            install(ConditionalHeaders)

            install(createApplicationPlugin(name = "CORS") {
                onCall { call ->
                    call.response.header(HttpHeaders.AccessControlAllowOrigin, "*")
                }
            })

            install(ContentNegotiation) {
                ignoreType<ThymeleafContent>()

                register(ContentType.Application.Json, JacksonConverter(mapper))
            }

            install(DefaultHeaders)

            install(Thymeleaf) {
                addDialect(ByteUnitsDialect)
                addDialect(Java8TimeDialect())

                setTemplateResolver(ClassLoaderTemplateResolver().apply {
                    prefix = "/org/openrs2/archive/templates/"
                    templateMode = TemplateMode.HTML
                })
            }

            install(XForwardedHeaders)
            install(Webjars)

            routing {
                get("/") { call.respond(ThymeleafContent("index.html", emptyMap())) }
                get("/api") { call.respond(ThymeleafContent("api/index.html", mapOf("active" to "api"))) }
                get("/caches") { cachesController.index(call) }
                get("/caches.json") { cachesController.indexJson(call) }
                get("/caches/{scope}/{id}") { cachesController.show(call) }
                get("/caches/{scope}/{id}/archives/{archive}/groups/{group}.dat") { cachesController.exportGroup(call) }
                get("/caches/{scope}/{id}/disk.zip") { cachesController.exportDisk(call) }
                get("/caches/{scope}/{id}/flat-file.tar.gz") { cachesController.exportFlatFile(call) }
                get("/caches/{scope}/{id}/keys.json") { cachesController.exportKeysJson(call) }
                get("/caches/{scope}/{id}/keys.zip") { cachesController.exportKeysZip(call) }
                get("/caches/{scope}/{id}/map.png") { cachesController.renderMap(call) }
                get("/clients") { clientsController.index(call) }
                get("/clients/{id}.dat") { clientsController.export(call) }
                get("/clients/{id}") { clientsController.show(call) }
                get("/keys") { keysController.index(call) }
                post("/keys") { keysController.import(call) }
                get("/keys/all.json") { keysController.exportAll(call) }
                get("/keys/valid.json") { keysController.exportValid(call) }
                staticResources("/static", "/org/openrs2/archive/static")

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
