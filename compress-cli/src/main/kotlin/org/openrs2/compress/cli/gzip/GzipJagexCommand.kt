package org.openrs2.compress.cli.gzip

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import org.openrs2.compress.gzip.JagexGzipOutputStream

public class GzipJagexCommand : CliktCommand(name = "gzip-jagex") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream(truncateExisting = true).defaultStdout()

    override fun run() {
        input.use { input ->
            JagexGzipOutputStream(output).use { output ->
                input.copyTo(output)
            }
        }
    }
}
