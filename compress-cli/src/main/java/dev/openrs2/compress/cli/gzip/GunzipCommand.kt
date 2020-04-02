package dev.openrs2.compress.cli.gzip

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import dev.openrs2.cli.defaultStdin
import dev.openrs2.cli.defaultStdout
import dev.openrs2.cli.inputStream
import dev.openrs2.cli.outputStream
import dev.openrs2.compress.gzip.Gzip

class GunzipCommand : CliktCommand(name = "gunzip") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        Gzip.createHeaderlessInputStream(input).use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
    }
}
