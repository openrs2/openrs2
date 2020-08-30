package dev.openrs2.cache

import io.netty.buffer.ByteBuf
import io.netty.buffer.ByteBufAllocator
import java.io.Closeable
import java.io.EOFException
import java.io.Flushable
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min

public class BufferedFileChannel(
    private val channel: FileChannel,
    readBufferSize: Int,
    writeBufferSize: Int,
    alloc: ByteBufAllocator = ByteBufAllocator.DEFAULT
) : Flushable, Closeable {
    private var size = channel.size()

    private val readBuffer: ByteBuf
    private var readPos = -1L

    private val writeBuffer: ByteBuf
    private var writePos = -1L

    init {
        val buf = alloc.buffer(readBufferSize, readBufferSize)
        try {
            writeBuffer = alloc.buffer(writeBufferSize, writeBufferSize)
            readBuffer = buf.retain()
        } finally {
            buf.release()
        }
    }

    public fun read(pos: Long, dest: ByteBuf, len: Int) {
        require(pos >= 0)
        require(len <= dest.writableBytes())

        val originalDestIndex = dest.writerIndex()

        var off = pos
        var remaining = len

        /*
         * Service the whole read from the write buffer, if we can. This code
         * isn't necessary, but it is more optimal than following the whole
         * sequence of reads below.
         */
        val writeLen = writeBuffer.readableBytes()
        if (writePos != -1L && off >= writePos && off + remaining <= writePos + writeLen) {
            val copyOff = (off - writePos).toInt()
            dest.writeBytes(writeBuffer, copyOff, remaining)
            return
        }

        // Service the first part of the read from the read buffer.
        if (readPos != -1L && off >= readPos && off < readPos + readBuffer.readableBytes()) {
            val copyOff = (off - readPos).toInt()
            val copyLen = min(readBuffer.readableBytes() - copyOff, remaining)

            dest.writeBytes(readBuffer, copyOff, copyLen)

            off += copyLen
            remaining -= copyLen
        }

        if (remaining > readBuffer.capacity()) {
            /*
             * If the remaining part of the read is larger than the read
             * buffer, read directly from the file into the destination buffer.
             */
            while (remaining > 0) {
                val n = dest.writeBytes(channel, off, remaining)
                if (n == -1) {
                    break
                }
                off += n
                remaining -= n
            }
        } else if (remaining > 0) {
            /*
             * Otherwise clear and repopulate the entire read buffer from the
             * current position, then copy into the destination buffer.
             */
            fill(off)

            val copyLen = min(readBuffer.readableBytes(), remaining)

            dest.writeBytes(readBuffer, 0, copyLen)

            off += copyLen
            remaining -= copyLen
        }

        if (writePos != -1L) {
            /*
             * If an unflushed write extended the length of the file, fill in
             * the gap between the current position and the write position with
             * zeroes to reflect what the filesystem would do.
             */
            if (off < writePos && remaining > 0) {
                val zeroLen = min((writePos - off).toInt(), remaining)

                dest.writeZero(zeroLen)

                off += zeroLen
                remaining -= zeroLen
            }

            /*
             * If a subset of the write buffer overlaps with a subset of the
             * destination buffer, overwrite that subset of the destination
             * buffer with the write buffer as the write buffer must take
             * precedence over the read buffer.
             */
            val start = if (writePos >= pos && writePos < pos + len) {
                writePos
            } else if (pos >= writePos && pos < writePos + writeLen) {
                pos
            } else {
                -1L
            }

            val end = if (writePos + writeLen > pos && writePos + writeLen <= pos + len) {
                writePos + writeLen
            } else if (pos + len > writePos && pos + len <= writePos + writeLen) {
                pos + len
            } else {
                -1L
            }

            if (start != -1L && end != -1L && start < end) {
                val destIndex = originalDestIndex + (start - pos).toInt()

                val copyOff = (start - writePos).toInt()
                val copyLen = (end - start).toInt()

                dest.setBytes(destIndex, writeBuffer, copyOff, copyLen)

                /*
                 * If we filled in any remaining bytes in the destination
                 * buffer from the write buffer then adjust the indexes to take
                 * that into account.
                 */
                if (end > off) {
                    val n = (end - off).toInt()

                    dest.writerIndex(dest.writerIndex() + n)

                    off += n
                    remaining -= n
                }
            }
        }

        if (remaining > 0) {
            throw EOFException()
        }
    }

    private fun fill(pos: Long) {
        require(pos >= 0)

        readBuffer.clear()
        readPos = pos

        var off = pos
        while (readBuffer.isWritable) {
            val n = readBuffer.writeBytes(channel, off, readBuffer.writableBytes())
            if (n == -1) {
                break
            }
            off += n
        }
    }

    public fun write(pos: Long, src: ByteBuf, len: Int) {
        require(pos >= 0)
        require(len <= src.readableBytes())

        size = max(size, pos + len.toLong())

        var off = pos
        var remaining = len

        /*
         * If the start of the write doesn't overlap with the write buffer,
         * flush the existing write buffer.
         */
        if (writePos != -1L && (off < writePos || off > writePos + writeBuffer.readableBytes())) {
            flush()
        }

        /*
         * If the start of the write does overlap with the write buffer
         * (implicit due to the if condition and flush() call above) and the
         * end of the write runs beyond the end of the write buffer, overwrite
         * the relevant part of the write buffer with the start of the source
         * buffer and then flush the whole write buffer.
         */
        if (writePos != -1L && off + remaining > writePos + writeBuffer.capacity()) {
            val copyOff = (off - writePos).toInt()
            val copyLen = writeBuffer.capacity() - copyOff

            src.readBytes(writeBuffer, copyOff, copyLen)

            off += copyLen
            remaining -= copyLen

            writeBuffer.writerIndex(writeBuffer.capacity())

            flush()
        }

        if (remaining > writeBuffer.capacity()) {
            /*
             * If the remaining part of the write is longer than the write
             * buffer, write directly to the underlying file.
             */
            val originalSrcIndex = src.readerIndex()
            writeFully(off, src, remaining)

            /*
             * If the write overlaps with the read buffer, update the relevant
             * portion of the read buffer. (As we bypassed the write buffer, we
             * can't rely on the write buffer taking precedence over the read
             * buffer.)
             */
            val readLen = readBuffer.readableBytes()

            val start = if (off >= readPos && off < readPos + readLen) {
                off
            } else if (readPos >= off && readPos < off + remaining) {
                readPos
            } else {
                -1L
            }

            val end = if (off + remaining > readPos && off + remaining <= readPos + readLen) {
                off + remaining
            } else if (readPos + readLen > off && readPos + readLen <= off + remaining) {
                readPos + readLen
            } else {
                -1L
            }

            if (start != -1L && end != -1L && start < end) {
                val srcIndex = originalSrcIndex + (start - off).toInt()

                val copyOff = (start - readPos).toInt()
                val copyLen = (end - start).toInt()

                src.getBytes(srcIndex, readBuffer, copyOff, copyLen)
            }
        } else if (remaining > 0) {
            // Otherwise write to the write buffer.
            if (writePos == -1L) {
                writePos = off
            }

            val copyOff = (off - writePos).toInt()

            src.readBytes(writeBuffer, copyOff, remaining)

            off += remaining

            // Increase write buffer length if necessary.
            val newWriteLen = (off - writePos).toInt()
            if (newWriteLen > writeBuffer.readableBytes()) {
                writeBuffer.writerIndex(newWriteLen)
            }
        }
    }

    private fun writeFully(pos: Long, src: ByteBuf, len: Int) {
        require(pos >= 0)
        require(len <= src.readableBytes())

        var off = pos
        var remaining = len

        while (remaining > 0) {
            val n = src.readBytes(channel, off, len)
            off += n
            remaining -= n
        }
    }

    public fun size(): Long {
        return size
    }

    override fun flush() {
        if (writePos != -1L) {
            writeFully(writePos, writeBuffer, writeBuffer.readableBytes())
            writeBuffer.clear()
            writePos = -1L
        }
    }

    override fun close() {
        flush()

        channel.close()

        readBuffer.release()
        writeBuffer.release()
    }
}
