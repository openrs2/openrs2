package org.openrs2.compress.cli.pack200

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import org.openrs2.compress.gzip.Gzip
import java.util.jar.JarInputStream
import java.util.jar.Pack200

public class Pack200Command : CliktCommand(name = "pack200") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        JarInputStream(input).use { jarInput ->
            Gzip.createHeaderlessOutputStream(output).use { gzipOutput ->
                Pack200.newPacker().pack(jarInput, gzipOutput)
            }
        }
    }
}
