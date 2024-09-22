package org.openrs2.patcher

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.google.inject.Guice
import org.openrs2.inject.CloseableInjector
import java.nio.file.Path

public fun main(args: Array<String>): Unit = PatchCommand().main(args)

public class PatchCommand : CliktCommand(name = "patch") {
    override fun run() {
        CloseableInjector(Guice.createInjector(PatcherModule)).use { injector ->
            val patcher = injector.getInstance(Patcher::class.java)
            patcher.run(
                input = Path.of("nonfree/lib"),
                output = Path.of("nonfree/var/cache/client"),
                keyStorePath = Path.of("etc/loader.p12")
            )
        }
    }
}
