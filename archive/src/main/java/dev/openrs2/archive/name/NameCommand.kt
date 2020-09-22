package dev.openrs2.archive.name

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public class NameCommand : NoOpCliktCommand(name = "name") {
    init {
        subcommands(
            GenerateCommand(),
            ImportCommand()
        )
    }
}
