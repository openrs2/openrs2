package dev.openrs2.compress.cli.gzip

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import dev.openrs2.compress.gzip.Gzip

public class GunzipCommand : CliktCommand(name = "gunzip") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream(truncateExisting = true).defaultStdout()

    override fun run() {
        Gzip.createHeaderlessInputStream(input).use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
    }
}
