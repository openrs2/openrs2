package org.openrs2

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.openrs2.archive.ArchiveCommand
import org.openrs2.buffer.generator.GenerateBufferCommand
import org.openrs2.cache.cli.CacheCommand
import org.openrs2.compress.cli.CompressCommand
import org.openrs2.crc32.Crc32Command
import org.openrs2.deob.DeobfuscateCommand
import org.openrs2.game.GameCommand
import org.openrs2.patcher.PatchCommand

public fun main(args: Array<String>): Unit = Command().main(args)

public class Command : NoOpCliktCommand(name = "openrs2") {
    init {
        subcommands(
            ArchiveCommand(),
            CacheCommand(),
            CompressCommand(),
            Crc32Command(),
            DeobfuscateCommand(),
            GameCommand(),
            GenerateBufferCommand(),
            PatchCommand()
        )
    }
}
