package org.openrs2.archive.web

import io.ktor.application.call
import io.ktor.application.install
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.thymeleaf.Thymeleaf
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect
import org.thymeleaf.templatemode.TemplateMode
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
public class WebServer @Inject constructor(
    private val cachesController: CachesController,
    private val keysController: KeysController
) {
    public fun start() {
        embeddedServer(Netty, port = 8000) {
            install(Thymeleaf) {
                addDialect(Java8TimeDialect())

                setTemplateResolver(ClassLoaderTemplateResolver().apply {
                    prefix = "/org/openrs2/archive/templates/"
                    templateMode = TemplateMode.HTML
                })
            }

            routing {
                get("/caches") { cachesController.index(call) }
                get("/caches/{id}.zip") { cachesController.export(call) }

                // ideally we'd use POST /keys here, but I want to be compatible with the RuneLite/OpenOSRS API
                get("/keys/submit") { keysController.create(call) }
            }
        }.start(wait = true)
    }
}
