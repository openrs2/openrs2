package org.openrs2.archive.masterindex

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public class MasterIndexCommand : NoOpCliktCommand(name = "master-index") {
    init {
        subcommands(
            ImportCommand()
        )
    }
}
