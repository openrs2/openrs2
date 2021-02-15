package org.openrs2.archive.name

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public class NameCommand : NoOpCliktCommand(name = "name") {
    init {
        subcommands(
            DownloadCommand(),
            GenerateCommand(),
            ImportCommand()
        )
    }
}
