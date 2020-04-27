package dev.openrs2.bundler

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import java.nio.file.Paths

fun main(args: Array<String>) = BundleCommand().main(args)

class BundleCommand : CliktCommand(name = "bundle") {
    override fun run() {
        val injector = Guice.createInjector(BundlerModule)
        val bundler = injector.getInstance(Bundler::class.java)
        bundler.run(Paths.get("nonfree/lib"), Paths.get("nonfree/var/cache/bundle"), Paths.get("etc/loader.p12"))
    }
}
