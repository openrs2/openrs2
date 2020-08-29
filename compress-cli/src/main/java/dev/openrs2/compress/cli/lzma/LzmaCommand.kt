package dev.openrs2.compress.cli.lzma

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.outputStream
import dev.openrs2.compress.lzma.Lzma
import org.tukaani.xz.LZMA2Options

public class LzmaCommand : CliktCommand(name = "lzma") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()
    private val level by option().int().default(LZMA2Options.PRESET_DEFAULT).validate {
        require(it >= LZMA2Options.PRESET_MIN && it <= LZMA2Options.PRESET_MAX) {
            "--level must be between ${LZMA2Options.PRESET_MIN} and ${LZMA2Options.PRESET_MAX} inclusive"
        }
    }

    override fun run() {
        input.use { input ->
            Lzma.createHeaderlessOutputStream(output, LZMA2Options(level)).use { output ->
                System.err.println(input.copyTo(output))
            }
        }
    }
}
