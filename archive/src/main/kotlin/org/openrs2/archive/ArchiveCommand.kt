package org.openrs2.archive

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.openrs2.archive.cache.CacheCommand
import org.openrs2.archive.key.KeyCommand
import org.openrs2.archive.masterindex.MasterIndexCommand
import org.openrs2.archive.name.NameCommand

public fun main(args: Array<String>): Unit = ArchiveCommand().main(args)

public class ArchiveCommand : NoOpCliktCommand(name = "archive") {
    init {
        subcommands(
            CacheCommand(),
            KeyCommand(),
            MasterIndexCommand(),
            NameCommand()
        )
    }
}
