package dev.openrs2.cache

import io.netty.buffer.ByteBuf
import java.io.Closeable
import java.io.EOFException
import java.io.Flushable
import java.nio.channels.FileChannel

// TODO(gpe): actually implement buffering
class BufferedFileChannel(
    private val channel: FileChannel
) : Flushable, Closeable {
    fun read(pos: Long, dest: ByteBuf, len: Int) {
        require(len <= dest.writableBytes())

        var off = pos
        var remaining = len

        while (remaining > 0) {
            val n = dest.writeBytes(channel, off, remaining)
            if (n == -1) {
                throw EOFException()
            }
            off += n
            remaining -= n
        }
    }

    fun write(pos: Long, src: ByteBuf, len: Int) {
        require(len <= src.readableBytes())

        var off = pos
        var remaining = len

        while (remaining > 0) {
            val n = src.readBytes(channel, off, remaining)
            off += n
            remaining -= n
        }
    }

    fun size(): Long {
        return channel.size()
    }

    override fun flush() {
        // empty
    }

    override fun close() {
        channel.close()
    }
}
