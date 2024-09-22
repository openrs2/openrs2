package org.openrs2.buffer.generator

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.main
import java.nio.file.Files
import java.nio.file.Path

public fun main(args: Array<String>): Unit = GenerateBufferCommand().main(args)

public class GenerateBufferCommand : CliktCommand(name = "generate-buffer") {
    override fun run() {
        Files.writeString(PATH, ByteBufExtensionGenerator().generate())
    }

    private companion object {
        private val PATH = Path.of("buffer/src/main/kotlin/org/openrs2/buffer/GeneratedByteBufExtensions.kt")
    }
}
