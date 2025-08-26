package org.openrs2.deob.ast

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.google.inject.Guice
import org.openrs2.deob.util.profile.ProfileModule
import org.openrs2.inject.CloseableInjector

public fun main(args: Array<String>): Unit = DeobfuscateAstCommand().main(args)

public class DeobfuscateAstCommand : CliktCommand(name = "ast") {
    private val profile by option().default("profile.yaml")

    override fun run() {
        CloseableInjector(Guice.createInjector(ProfileModule(profile), AstDeobfuscatorModule)).use { injector ->
            val deobfuscator = injector.getInstance(AstDeobfuscator::class.java)
            deobfuscator.run()
        }
    }
}
