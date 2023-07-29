package org.openrs2.compress.cli.pack200

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.defaultStdout
import com.github.ajalt.clikt.parameters.types.inputStream
import com.github.ajalt.clikt.parameters.types.outputStream
import org.openrs2.compress.gzip.Gzip
import java.util.jar.JarOutputStream
import java.util.jar.Pack200

public class Unpack200Command : CliktCommand(name = "unpack200") {
    private val input by option().inputStream().defaultStdin()
    private val output by option().outputStream().defaultStdout()

    override fun run() {
        Gzip.createHeaderlessInputStream(input).use { gzipInput ->
            JarOutputStream(output).use { jarOutput ->
                Pack200.newUnpacker().unpack(gzipInput, jarOutput)
            }
        }
    }
}
