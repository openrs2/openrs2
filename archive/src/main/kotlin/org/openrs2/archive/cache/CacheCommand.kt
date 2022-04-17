package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands
import org.openrs2.archive.cache.finder.ExtractCommand

public class CacheCommand : NoOpCliktCommand(name = "cache") {
    init {
        subcommands(
            DownloadCommand(),
            ExtractCommand(),
            ImportCommand(),
            ImportMasterIndexCommand(),
            ExportCommand(),
            RefreshViewsCommand()
        )
    }
}
