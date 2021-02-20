package org.openrs2.deob

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import org.openrs2.inject.CloseableInjector

public fun main(args: Array<String>): Unit = DeobfuscateCommand().main(args)

public class DeobfuscateCommand : CliktCommand(name = "deob") {
    override fun run() {
        CloseableInjector(Guice.createInjector(DeobfuscatorModule)).use { injector ->
            val deobfuscator = injector.getInstance(Deobfuscator::class.java)
            deobfuscator.run()
        }
    }
}
