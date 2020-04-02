package dev.openrs2.compress.cli.bzip2

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import dev.openrs2.cli.defaultStdin
import dev.openrs2.cli.defaultStdout
import dev.openrs2.cli.inputStream
import dev.openrs2.cli.outputStream
import dev.openrs2.compress.bzip2.Bzip2

class Bzip2Command : CliktCommand(name = "bzip2") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        input.use { input ->
            Bzip2.createHeaderlessOutputStream(output).use { output ->
                input.copyTo(output)
            }
        }
    }
}
