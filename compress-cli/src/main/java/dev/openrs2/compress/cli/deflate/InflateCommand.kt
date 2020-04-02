package dev.openrs2.compress.cli.deflate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import dev.openrs2.cli.defaultStdin
import dev.openrs2.cli.defaultStdout
import dev.openrs2.cli.inputStream
import dev.openrs2.cli.outputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

class InflateCommand : CliktCommand(name = "inflate") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        InflaterInputStream(input, Inflater(true)).use { input ->
            output.use { output ->
                input.copyTo(output)
            }
        }
    }
}
