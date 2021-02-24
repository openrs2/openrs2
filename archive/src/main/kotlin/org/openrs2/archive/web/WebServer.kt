package org.openrs2.archive.web

import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.jackson.JacksonConverter
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.thymeleaf.Thymeleaf
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

            install(Webjars)

            routing {
                get("/caches") { cachesController.index(call) }
                get("/caches/{id}") { cachesController.show(call) }
                get("/caches/{id}.zip") { cachesController.export(call) }
                get("/caches/{id}.json") { cachesController.exportKeys(call) }
            }
        }.start(wait = true)
    }
}
