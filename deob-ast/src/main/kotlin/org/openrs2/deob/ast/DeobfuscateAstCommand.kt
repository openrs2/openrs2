package org.openrs2.deob.ast

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import org.openrs2.deob.util.Module

public fun main(args: Array<String>): Unit = DeobfuscateAstCommand().main(args)

public class DeobfuscateAstCommand : CliktCommand(name = "deob-ast") {
    override fun run() {
        val injector = Guice.createInjector(AstDeobfuscatorModule)
        val deobfuscator = injector.getInstance(AstDeobfuscator::class.java)
        deobfuscator.run(Module.ALL)
    }
}
