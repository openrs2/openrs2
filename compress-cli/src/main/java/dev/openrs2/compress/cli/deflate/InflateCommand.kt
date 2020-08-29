package dev.openrs2.compress.cli.deflate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import java.util.zip.Inflater
import java.util.zip.InflaterInputStream

public class InflateCommand : CliktCommand(name = "inflate") {
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
