package dev.openrs2.compress.cli.lzma

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.long
import com.github.ajalt.clikt.parameters.types.outputStream
import dev.openrs2.compress.lzma.Lzma

public class UnlzmaCommand : CliktCommand(name = "unlzma") {
    private val input by option().inputStream().defaultStdin()
    private val length by option().long().required()
    private val output by option().outputStream(truncateExisting = true).defaultStdout()

    override fun run() {
        Lzma.createHeaderlessInputStream(input, length).use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
    }
}
