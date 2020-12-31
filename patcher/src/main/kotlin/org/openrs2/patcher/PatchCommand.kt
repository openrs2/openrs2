package org.openrs2.patcher

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import java.nio.file.Paths

public fun main(args: Array<String>): Unit = PatchCommand().main(args)

public class PatchCommand : CliktCommand(name = "patch") {
    override fun run() {
        val injector = Guice.createInjector(PatcherModule)
        val patcher = injector.getInstance(Patcher::class.java)
        patcher.run(
            input = Paths.get("nonfree/lib"),
            output = Paths.get("nonfree/var/cache/client"),
            keyStorePath = Paths.get("etc/loader.p12")
        )
    }
}
