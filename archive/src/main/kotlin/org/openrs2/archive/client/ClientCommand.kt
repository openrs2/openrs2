package org.openrs2.archive.client

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public class ClientCommand : NoOpCliktCommand(name = "client") {
    init {
        subcommands(
            ExportCommand(),
            ImportCommand(),
            RefreshCommand()
        )
    }
}
