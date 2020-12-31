package org.openrs2.deob.bytecode

import com.github.ajalt.clikt.core.CliktCommand
import com.google.inject.Guice
import java.nio.file.Paths

public fun main(args: Array<String>): Unit = DeobfuscateBytecodeCommand().main(args)

public class DeobfuscateBytecodeCommand : CliktCommand(name = "deob-bytecode") {
    override fun run() {
        val injector = Guice.createInjector(BytecodeDeobfuscatorModule)
        val deobfuscator = injector.getInstance(BytecodeDeobfuscator::class.java)
        deobfuscator.run(
            input = Paths.get("nonfree/lib"),
            output = Paths.get("nonfree/var/cache/deob")
        )
    }
}
