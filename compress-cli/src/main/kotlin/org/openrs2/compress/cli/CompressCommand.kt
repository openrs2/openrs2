package org.openrs2.compress.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.openrs2.compress.cli.bzip2.Bunzip2Command
import org.openrs2.compress.cli.bzip2.Bzip2Command
import org.openrs2.compress.cli.deflate.DeflateCommand
import org.openrs2.compress.cli.deflate.InflateCommand
import org.openrs2.compress.cli.gzip.GunzipCommand
import org.openrs2.compress.cli.gzip.GunzipLaxCommand
import org.openrs2.compress.cli.gzip.GzipCommand
import org.openrs2.compress.cli.gzip.GzipJagexCommand
import org.openrs2.compress.cli.lzma.LzmaCommand
import org.openrs2.compress.cli.lzma.UnlzmaCommand
import org.openrs2.compress.cli.pack200.Pack200Command
import org.openrs2.compress.cli.pack200.Unpack200Command
import org.openrs2.compress.cli.packclass.PackClassCommand
import org.openrs2.compress.cli.packclass.UnpackClassCommand

public fun main(args: Array<String>): Unit = CompressCommand().main(args)

public class CompressCommand : NoOpCliktCommand(name = "compress") {
    init {
        subcommands(
            Bzip2Command(),
            Bunzip2Command(),
            DeflateCommand(),
            InflateCommand(),
            GzipCommand(),
            GzipJagexCommand(),
            GunzipCommand(),
            GunzipLaxCommand(),
            LzmaCommand(),
            UnlzmaCommand(),
            Pack200Command(),
            Unpack200Command(),
            PackClassCommand(),
            UnpackClassCommand()
        )
    }
}
