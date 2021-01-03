package org.openrs2.deob

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice

public fun main(args: Array<String>): Unit = DeobfuscateCommand().main(args)

public class DeobfuscateCommand : CliktCommand(name = "deob") {
    override fun run() {
        val injector = Guice.createInjector(DeobfuscatorModule)
        val deobfuscator = injector.getInstance(Deobfuscator::class.java)
        deobfuscator.run()
    }
}
