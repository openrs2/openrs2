package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public class CacheCommand : NoOpCliktCommand(name = "cache") {
    init {
        subcommands(
            DownloadCommand(),
            ImportCommand(),
            ImportMasterIndexCommand(),
            ExportCommand(),
            RefreshViewsCommand()
        )
    }
}
