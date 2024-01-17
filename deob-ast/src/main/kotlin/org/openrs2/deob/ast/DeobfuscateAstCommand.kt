package org.openrs2.deob.ast

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import org.openrs2.deob.util.Module
import org.openrs2.inject.CloseableInjector

public fun main(args: Array<String>): Unit = DeobfuscateAstCommand().main(args)

public class DeobfuscateAstCommand : CliktCommand(name = "ast") {
    override fun run() {
        CloseableInjector(Guice.createInjector(AstDeobfuscatorModule)).use { injector ->
            // val deobfuscator = injector.getInstance(AstDeobfuscator::class.java)
            // deobfuscator.run(Module.ALL)
        }
    }
}
