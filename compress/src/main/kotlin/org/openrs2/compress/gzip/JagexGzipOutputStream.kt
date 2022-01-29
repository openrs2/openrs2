package org.openrs2.compress.gzip

import com.google.common.io.LittleEndianDataOutputStream
import java.io.DataOutputStream
import java.io.OutputStream
import java.util.zip.CRC32
import java.util.zip.Deflater

public class JagexGzipOutputStream(
    private val output: OutputStream
) : OutputStream() {
    private val deflater = Deflater(Deflater.DEFAULT_COMPRESSION, true)
    private val buffer = ByteArray(4096)
    private val checksum = CRC32()
    private var size = 0
    private var closed = false

    init {
        val dataOutput = DataOutputStream(output)
        dataOutput.writeShort(HEADER_MAGIC)
        dataOutput.writeByte(METHOD_DEFLATE)
        dataOutput.writeByte(0) // FLG
        dataOutput.writeInt(0) // MTIME
        dataOutput.writeByte(0) // XFL
        dataOutput.writeByte(0) // OS
    }

    override fun write(b: Int) {
        buffer[0] = b.toByte()
        write(buffer, 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        checksum.update(b, off, len)
        size += len

        deflater.setInput(b, off, len)

        while (!deflater.needsInput()) {
            drain()
        }
    }

    private fun drain() {
        val n = deflater.deflate(buffer)
        if (n != 0) {
            output.write(buffer, 0, n)
        }
    }

    override fun close() {
        if (!closed) {
            deflater.finish()

            while (!deflater.finished()) {
                drain()
            }

            val dataOutput = LittleEndianDataOutputStream(output)
            dataOutput.writeInt(checksum.value.toInt())
            dataOutput.writeInt(size)

            deflater.end()
            output.close()

            closed = true
        }
    }

    private companion object {
        private const val HEADER_MAGIC = 0x1F8B

        private const val METHOD_DEFLATE = 8
    }
}
