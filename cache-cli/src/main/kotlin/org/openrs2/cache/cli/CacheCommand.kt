package org.openrs2.cache.cli

import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.subcommands

public fun main(args: Array<String>): Unit = CacheCommand().main(args)

public class CacheCommand : NoOpCliktCommand(name = "cache") {
    init {
        subcommands(
            RuneLiteUnpackCommand()
        )
    }
}
