package org.openrs2.archive.key

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public class KeyCommand : NoOpCliktCommand(name = "key") {
    init {
        subcommands(
            BruteForceCommand(),
            DownloadCommand(),
            EntCommand(),
            ImportCommand()
        )
    }
}
