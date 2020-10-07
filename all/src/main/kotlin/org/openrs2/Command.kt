package org.openrs2

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.openrs2.archive.ArchiveCommand
import org.openrs2.bundler.BundleCommand
import org.openrs2.compress.cli.CompressCommand
import org.openrs2.crc32.Crc32Command
import org.openrs2.decompiler.DecompileCommand
import org.openrs2.deob.DeobfuscateCommand
import org.openrs2.deob.ast.DeobfuscateAstCommand
import org.openrs2.game.GameCommand

public fun main(args: Array<String>): Unit = Command().main(args)

public class Command : NoOpCliktCommand(name = "openrs2") {
    init {
        subcommands(
            ArchiveCommand(),
            BundleCommand(),
            CompressCommand(),
            Crc32Command(),
            DecompileCommand(),
            DeobfuscateCommand(),
            DeobfuscateAstCommand(),
            GameCommand()
        )
    }
}
