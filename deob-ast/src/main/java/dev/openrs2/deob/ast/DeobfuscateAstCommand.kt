package dev.openrs2.deob.ast

import com.github.ajalt.clikt.core.CliktCommand
import java.nio.file.Paths

fun main(args: Array<String>) = DeobfuscateAstCommand().main(args)

class DeobfuscateAstCommand : CliktCommand(name = "deob-ast") {
    override fun run() {
        val deobfuscator = AstDeobfuscator(
            listOf(
                Paths.get("nonfree/client/src/main/java"),
                Paths.get("nonfree/gl/src/main/java"),
                Paths.get("nonfree/loader/src/main/java"),
                Paths.get("nonfree/signlink/src/main/java"),
                Paths.get("nonfree/unpack/src/main/java"),
                Paths.get("nonfree/unpackclass/src/main/java")
            )
        )
        deobfuscator.run()
    }
}
