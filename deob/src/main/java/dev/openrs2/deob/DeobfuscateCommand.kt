package dev.openrs2.deob

import com.github.ajalt.clikt.core.CliktCommand
import java.nio.file.Paths

fun main(args: Array<String>) = DeobfuscateCommand().main(args)

class DeobfuscateCommand : CliktCommand(name = "deob") {
    override fun run() {
        val deobfuscator = Deobfuscator(Paths.get("nonfree/code"), Paths.get("nonfree/code/deob"))
        deobfuscator.run()
    }
}
