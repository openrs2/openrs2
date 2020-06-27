package dev.openrs2.deob.ast

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import dev.openrs2.deob.util.Module

fun main(args: Array<String>) = DeobfuscateAstCommand().main(args)

class DeobfuscateAstCommand : CliktCommand(name = "deob-ast") {
    override fun run() {
        val injector = Guice.createInjector(AstDeobfuscatorModule)
        val deobfuscator = injector.getInstance(AstDeobfuscator::class.java)
        deobfuscator.run(Module.all)
    }
}
