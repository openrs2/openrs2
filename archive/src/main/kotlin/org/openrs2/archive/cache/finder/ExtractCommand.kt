package org.openrs2.archive.cache.finder

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.default
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.path
import java.nio.file.Path

public class ExtractCommand : CliktCommand(name = "extract") {
    private val input by argument().inputStream()
    private val output by argument().path(
        mustExist = false,
        canBeFile = false,
        canBeDir = true,
        mustBeReadable = true,
        mustBeWritable = true
    ).default(Path.of("."))

    override fun run() {
        CacheFinderExtractor(input).use { extractor ->
            extractor.extract(output)
        }
    }
}
