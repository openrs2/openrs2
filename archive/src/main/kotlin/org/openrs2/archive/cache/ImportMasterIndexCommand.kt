package org.openrs2.archive.cache

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.enum
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.clikt.parameters.types.path
import com.google.inject.Guice
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlinx.coroutines.runBlocking
import org.openrs2.archive.ArchiveModule
import org.openrs2.buffer.use
import org.openrs2.cache.Js5CompressionType
import org.openrs2.cache.MasterIndexFormat
import org.openrs2.cli.instant
import org.openrs2.inject.CloseableInjector
import java.io.IOException
import java.nio.file.Files
import kotlin.math.min

public class ImportMasterIndexCommand : CliktCommand(name = "import-master-index") {
    private val buildMajor by option().int()
    private val buildMinor by option().int()
    private val timestamp by option().instant()
    private val name by option()
    private val description by option()
    private val url by option()
    private val environment by option().default("live")
    private val language by option().default("en")
    private val decodeJs5Response by option().flag()

    private val game by argument()
    private val format by argument().enum<MasterIndexFormat>()
    private val input by argument().path(
        mustExist = true,
        canBeDir = false,
        mustBeReadable = true
    )

    override fun run(): Unit = runBlocking {
        CloseableInjector(Guice.createInjector(ArchiveModule)).use { injector ->
            val importer = injector.getInstance(CacheImporter::class.java)

            Unpooled.wrappedBuffer(Files.readAllBytes(input)).use { buf ->
                if (decodeJs5Response) {
                    decodeJs5Response(buf)
                } else {
                    buf.retain()
                }.use { decodedBuf ->
                    importer.importMasterIndex(
                        decodedBuf,
                        format,
                        game,
                        environment,
                        language,
                        buildMajor,
                        buildMinor,
                        timestamp,
                        name,
                        description,
                        url
                    )
                }
            }
        }
    }

    private fun decodeJs5Response(input: ByteBuf): ByteBuf {
        input.skipBytes(3) // archive and group

        val compression = input.readUnsignedByte().toInt()
        val len = input.readInt()
        if (len < 0) {
            throw IOException("Length is negative: $len")
        }

        val lenWithHeader = if (compression == Js5CompressionType.UNCOMPRESSED.ordinal) {
            len + 5
        } else {
            len + 9
        }

        input.alloc().buffer(lenWithHeader, lenWithHeader).use { output ->
            output.writeByte(compression)
            output.writeInt(len)

            var blockLen = 504
            while (true) {
                val n = min(blockLen, output.writableBytes())
                if (input.readableBytes() < n) {
                    throw IOException("Input truncated (expecting $n bytes, got ${input.readableBytes()})")
                }

                output.writeBytes(input, n)

                if (!output.isWritable) {
                    break
                } else if (!input.isReadable) {
                    throw IOException("Input truncated (expecting block trailer)")
                }

                if (input.readUnsignedByte().toInt() != 0xFF) {
                    throw IOException("Invalid block trailer")
                }

                blockLen = 511
            }

            return output.retain()
        }
    }
}
