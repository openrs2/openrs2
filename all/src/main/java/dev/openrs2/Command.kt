package dev.openrs2

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.openrs2.bundler.BundleCommand
import dev.openrs2.compress.cli.CompressCommand
import dev.openrs2.crc32.Crc32Command
import dev.openrs2.decompiler.DecompileCommand
import dev.openrs2.deob.DeobfuscateCommand
import dev.openrs2.deob.ast.DeobfuscateAstCommand
import dev.openrs2.game.GameCommand

fun main(args: Array<String>) = Command().main(args)

class Command : CliktCommand(name = "openrs2") {
    init {
        subcommands(
            BundleCommand(),
            CompressCommand(),
            Crc32Command(),
            DecompileCommand(),
            DeobfuscateCommand(),
            DeobfuscateAstCommand(),
            GameCommand()
        )
    }

    override fun run() {
        // empty
    }
}
