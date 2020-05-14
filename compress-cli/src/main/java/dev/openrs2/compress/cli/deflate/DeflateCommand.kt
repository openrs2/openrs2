package dev.openrs2.compress.cli.deflate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.outputStream
import java.util.zip.Deflater
import java.util.zip.DeflaterOutputStream

class DeflateCommand : CliktCommand(name = "deflate") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()
    private val level by option().int().default(Deflater.BEST_COMPRESSION).validate {
        require(it >= Deflater.NO_COMPRESSION && it <= Deflater.BEST_COMPRESSION) {
            "--level must be between ${Deflater.NO_COMPRESSION} and ${Deflater.BEST_COMPRESSION} inclusive"
        }
    }

    override fun run() {
        input.use { input ->
            DeflaterOutputStream(output, Deflater(level, true)).use { output ->
                input.copyTo(output)
            }
        }
    }
}
