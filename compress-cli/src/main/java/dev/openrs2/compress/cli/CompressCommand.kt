package dev.openrs2.compress.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import dev.openrs2.compress.cli.bzip2.Bunzip2Command
import dev.openrs2.compress.cli.bzip2.Bzip2Command
import dev.openrs2.compress.cli.deflate.DeflateCommand
import dev.openrs2.compress.cli.deflate.InflateCommand
import dev.openrs2.compress.cli.gzip.GunzipCommand
import dev.openrs2.compress.cli.gzip.GzipCommand

fun main(args: Array<String>) = CompressCommand().main(args)

class CompressCommand : NoOpCliktCommand(name = "compress") {
    init {
        subcommands(
            Bzip2Command(),
            Bunzip2Command(),
            DeflateCommand(),
            InflateCommand(),
            GzipCommand(),
            GunzipCommand()
        )
    }
}
