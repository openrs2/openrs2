package org.openrs2.archive

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.openrs2.archive.cache.CacheCommand
import org.openrs2.archive.client.ClientCommand
import org.openrs2.archive.key.KeyCommand
import org.openrs2.archive.name.NameCommand
import org.openrs2.archive.web.WebCommand

public fun main(args: Array<String>): Unit = ArchiveCommand().main(args)

public class ArchiveCommand : NoOpCliktCommand(name = "archive") {
    init {
        subcommands(
            CacheCommand(),
            ClientCommand(),
            KeyCommand(),
            NameCommand(),
            WebCommand()
        )
    }
}
