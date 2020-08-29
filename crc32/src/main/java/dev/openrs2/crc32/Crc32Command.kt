package dev.openrs2.crc32

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.defaultStdin
import com.github.ajalt.clikt.parameters.types.inputStream
import java.util.zip.CRC32

public fun main(args: Array<String>): Unit = Crc32Command().main(args)

public class Crc32Command : CliktCommand(name = "crc32") {
    private val input by option().inputStream().defaultStdin()

    override fun run() {
        val crc = CRC32()
        val bytes = ByteArray(4096)

        input.use { input ->
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
