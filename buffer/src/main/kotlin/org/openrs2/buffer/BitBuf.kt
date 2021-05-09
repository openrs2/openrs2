package org.openrs2.buffer

import com.google.common.base.Preconditions
import io.netty.buffer.ByteBuf
import kotlin.math.min

public class BitBuf(
    private val buf: ByteBuf
) : AutoCloseable {
    private var readerIndex: Long = buf.readerIndex().toLong() shl 3
        private set(value) {
            field = value
            buf.readerIndex((readerIndex shr 3).toInt())
        }

    private var writerIndex: Long = buf.writerIndex().toLong() shl 3
        private set(value) {
            field = value
            buf.writerIndex((writerIndex shr 3).toInt())
        }

    public fun getBoolean(index: Long): Boolean {
        return getBits(index, 1) != 0
    }

    public fun getBit(index: Long): Int {
        return getBits(index, 1)
    }

    public fun getBits(index: Long, len: Int): Int {
        Preconditions.checkArgument(len in 1..32)

        if (index < 0 || (index + len) > capacity()) {
            throw IndexOutOfBoundsException()
        }

        var value = 0

        var remaining = len
        var byteIndex = (index shr 3).toInt()
        var bitIndex = (index and 7).toInt()

        while (remaining > 0) {
            val n = min(8 - bitIndex, remaining)
            val shift = (8 - (bitIndex + n)) and 7
            val mask = (1 shl n) - 1

            val v = buf.getUnsignedByte(byteIndex).toInt()
            value = value shl n
            value = value or ((v shr shift) and mask)

            remaining -= n
            byteIndex++
            bitIndex = 0
        }

        return value
    }

    public fun readBoolean(): Boolean {
        return readBits(1) != 0
    }

    public fun readBit(): Int {
        return readBits(1)
    }

    public fun readBits(len: Int): Int {
        checkReadableBits(len)

        val value = getBits(readerIndex, len)
        readerIndex += len
        return value
    }

    public fun skipBits(len: Int): BitBuf {
        checkReadableBits(len)
        readerIndex += len

        return this
    }

    public fun setBoolean(index: Long, value: Boolean): BitBuf {
        if (value) {
            setBits(index, 1, 1)
        } else {
            setBits(index, 1, 0)
        }

        return this
    }

    public fun setBit(index: Long, value: Int): BitBuf {
        setBits(index, 1, value)

        return this
    }

    public fun setBits(index: Long, len: Int, value: Int): BitBuf {
        Preconditions.checkArgument(len in 1..32)

        if (index < 0 || (index + len) > capacity()) {
            throw IndexOutOfBoundsException()
        }

        var remaining = len
        var byteIndex = (index shr 3).toInt()
        var bitIndex = (index and 7).toInt()

        while (remaining > 0) {
            val n = min(8 - bitIndex, remaining)
            val shift = (8 - (bitIndex + n)) and 7
            val mask = (1 shl n) - 1

            var v = buf.getUnsignedByte(byteIndex).toInt()
            v = v and (mask shl shift).inv()
            v = v or (((value shr (remaining - n)) and mask) shl shift)
            buf.setByte(byteIndex, v)

            remaining -= n
            byteIndex++
            bitIndex = 0
        }

        return this
    }

    public fun writeBoolean(value: Boolean): BitBuf {
        if (value) {
            writeBits(1, 1)
        } else {
            writeBits(1, 0)
        }

        return this
    }

    public fun writeBit(value: Int): BitBuf {
        writeBits(1, value)

        return this
    }

    public fun writeBits(len: Int, value: Int): BitBuf {
        ensureWritable(len.toLong())

        setBits(writerIndex, len, value)
        writerIndex += len

        return this
    }

    public fun writeZero(len: Int): BitBuf {
        writeBits(len, 0)

        return this
    }

    private fun checkReadableBits(len: Int) {
        Preconditions.checkArgument(len >= 0)

        if ((readerIndex + len) > writerIndex) {
            throw IndexOutOfBoundsException()
        }
    }

    public fun ensureWritable(len: Long): BitBuf {
        Preconditions.checkArgument(len >= 0)

        if ((writerIndex + len) > maxCapacity()) {
            throw IndexOutOfBoundsException()
        }

        val currentByteIndex = writerIndex shr 3
        val nextByteIndex = (writerIndex + len + 7) shr 3

        buf.ensureWritable((nextByteIndex - currentByteIndex).toInt())

        return this
    }

    public fun readableBits(): Long {
        return writerIndex - readerIndex
    }

    public fun writableBits(): Long {
        return capacity() - writerIndex
    }

    public fun maxWritableBits(): Long {
        return maxCapacity() - writerIndex
    }

    public fun capacity(): Long {
        return buf.capacity().toLong() shl 3
    }

    public fun capacity(len: Long): BitBuf {
        buf.capacity((len shr 3).toInt())
        return this
    }

    public fun maxCapacity(): Long {
        return buf.maxCapacity().toLong() shl 3
    }

    public fun isReadable(): Boolean {
        return readerIndex < writerIndex
    }

    public fun isReadable(len: Long): Boolean {
        Preconditions.checkArgument(len >= 0)
        return (readerIndex + len) <= writerIndex
    }

    public fun isWritable(): Boolean {
        return writerIndex < capacity()
    }

    public fun isWritable(len: Long): Boolean {
        Preconditions.checkArgument(len >= 0)
        return (writerIndex + len) <= capacity()
    }

    public fun readerIndex(): Long {
        return readerIndex
    }

    public fun readerIndex(index: Long): BitBuf {
        if (index < 0 || index > writerIndex) {
            throw IndexOutOfBoundsException()
        }

        readerIndex = index
        return this
    }

    public fun writerIndex(): Long {
        return writerIndex
    }

    public fun writerIndex(index: Long): BitBuf {
        if (index < readerIndex || index > capacity()) {
            throw IndexOutOfBoundsException()
        }

        writerIndex = index
        return this
    }

    public fun clear(): BitBuf {
        readerIndex = 0
        writerIndex = 0
        return this
    }

    override fun close() {
        val bits = (((writerIndex + 7) and 7.toLong().inv()) - writerIndex).toInt()
        if (bits != 0) {
            writeZero(bits)
        }

        readerIndex = (readerIndex + 7) and 7.toLong().inv()
    }
}
