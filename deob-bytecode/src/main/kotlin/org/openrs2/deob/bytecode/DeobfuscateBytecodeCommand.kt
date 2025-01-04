package org.openrs2.deob.bytecode

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.google.inject.Guice
import org.openrs2.deob.util.profile.ProfileModule
import org.openrs2.inject.CloseableInjector
import java.nio.file.Path

public fun main(args: Array<String>): Unit = DeobfuscateBytecodeCommand().main(args)

public class DeobfuscateBytecodeCommand : CliktCommand(name = "bytecode") {
    private val profile by option().default("profile.yaml")

    override fun run() {
        CloseableInjector(Guice.createInjector(ProfileModule(profile), BytecodeDeobfuscatorModule)).use { injector ->
            val deobfuscator = injector.getInstance(BytecodeDeobfuscator::class.java)
            deobfuscator.run(
                input = Path.of("nonfree/lib"),
                output = Path.of("nonfree/var/cache/deob")
            )
        }
    }
}
