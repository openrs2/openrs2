package org.openrs2.compress.cli.bzip2

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import org.openrs2.compress.bzip2.Bzip2

public class Bunzip2Command : CliktCommand(name = "bunzip2") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream(truncateExisting = true).defaultStdout()

    override fun run() {
        Bzip2.createHeaderlessInputStream(input).use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
    }
}
