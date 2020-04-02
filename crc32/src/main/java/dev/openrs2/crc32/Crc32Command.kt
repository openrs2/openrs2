package dev.openrs2.crc32

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import dev.openrs2.cli.defaultStdin
import dev.openrs2.cli.inputStream
import java.util.zip.CRC32

fun main(args: Array<String>) = Crc32Command().main(args)

class Crc32Command : CliktCommand(name = "crc32") {
    private val input by option().inputStream().defaultStdin()

    override fun run() {
        val crc = CRC32()

        input.use { input ->
            val bytes = ByteArray(4096)

            while (true) {
                val len = input.read(bytes)
                if (len == -1) {
                    break
                }

                crc.update(bytes, 0, len)
            }
        }

        echo(crc.value.toInt())
    }
}
