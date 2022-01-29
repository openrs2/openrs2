package org.openrs2.compress.bzip2

import java.io.FilterOutputStream
import java.io.OutputStream
import java.util.zip.CRC32

public class Bzip2OutputStream(
    output: OutputStream,
    private val blockSize100k: Int,
    private val workFactor: Int = DEFAULT_WORK_FACTOR
) : FilterOutputStream(output) {
    // bit stream to byte stream encoder
    private val output = BitOutputStream(output)

    // used to implement write(int) efficiently
    private val temp = ByteArray(1)

    // current uncompressed block
    private val block: ByteArray
    private val blockCrc = CRC32()
    private var blockSize = 0 // nblock
    private var blockSizeMax: Int // nblockMAX

    private var combinedCrc = 0
    private var origPtr = 0
    private val ftab = IntArray(65537)
    private val ptr: IntArray

    // run length encoder
    private var stateInCh = 256
    private var stateInLen = 0

    private val inUse = BooleanArray(256)

    init {
        require(blockSize100k in 1..9)
        require(workFactor in 1..250)

        val n = blockSize100k * 100_000
        block = ByteArray(n)
        blockSizeMax = n - 19
        ptr = IntArray(n)

        writeHeader()
    }

    override fun write(b: Int) {
        temp[0] = b.toByte()
        write(temp)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        // run length encode input, compress when block is almost full
        for (i in off until off + len) {
            val ch = b[i].toInt() and 0xFF

            if (ch != stateInCh && stateInLen == 1) {
                // optimize common case
                blockCrc.update(stateInCh)
                inUse[stateInCh] = true
                block[blockSize++] = stateInCh.toByte()
                stateInCh = ch
            } else if (ch != stateInCh || stateInLen == 255) {
                flushRunLength()

                stateInCh = ch
                stateInLen = 1
            } else {
                stateInLen++
            }

            if (blockSize >= blockSizeMax) {
                compressBlock()
            }
        }
    }

    private fun flushRunLength() {
        if (stateInCh == 256) {
            return
        }

        for (i in 0 until stateInLen) {
            blockCrc.update(stateInCh)
        }

        inUse[stateInCh] = true

        when (stateInLen) {
            1 -> {
                block[blockSize++] = stateInCh.toByte()
            }
            2 -> {
                block[blockSize++] = stateInCh.toByte()
                block[blockSize++] = stateInCh.toByte()
            }
            3 -> {
                block[blockSize++] = stateInCh.toByte()
                block[blockSize++] = stateInCh.toByte()
                block[blockSize++] = stateInCh.toByte()
            }
            else -> {
                block[blockSize++] = stateInCh.toByte()
                block[blockSize++] = stateInCh.toByte()
                block[blockSize++] = stateInCh.toByte()
                block[blockSize++] = stateInCh.toByte()

                val runLength = stateInLen - 4
                inUse[runLength] = true
                block[blockSize++] = runLength.toByte()
            }
        }
    }

    override fun flush() {
        flushRunLength()
        compressBlock()
    }

    override fun close() {
        flush()
        writeTrailer()
        output.close()
    }

    private fun writeHeader() {
        // stream header
        output.writeByte('B'.code)
        output.writeByte('Z'.code)
        output.writeByte('h'.code)
        output.writeByte(('0' + blockSize100k).code)
    }

    private fun compressBlock() {
        if (blockSize != 0) {
            blockSort()

            // block magic
            output.writeByte(0x31)
            output.writeByte(0x41)
            output.writeByte(0x59)
            output.writeByte(0x26)
            output.writeByte(0x53)
            output.writeByte(0x59)

            // block checksum
            val blockCrc = blockCrc.value.toInt()
            combinedCrc = (combinedCrc shl 1) or (combinedCrc ushr 31)
            combinedCrc = combinedCrc xor blockCrc

            output.writeInt(blockCrc)

            // not randomised
            output.writeBoolean(false)

            // original pointer
            output.writeBits(24, origPtr)

            TODO() // MTF
        }

        // reset state ready for next block
        blockCrc.reset()
        blockSize = 0
        stateInCh = 256
        stateInLen = 0
        inUse.fill(false)
    }

    private fun writeTrailer() {
        // stream magic
        output.writeByte(0x17)
        output.writeByte(0x72)
        output.writeByte(0x45)
        output.writeByte(0x38)
        output.writeByte(0x50)
        output.writeByte(0x90)

        // stream checksum
        output.writeInt(combinedCrc)
    }

    private fun blockSort() {
        fallbackSort() // TODO: add mainSort() support

        origPtr = -1

        for (i in 0 until blockSize) {
            if (ptr[i] == 0) {
                origPtr = i
                break
            }
        }

        check(origPtr != -1)
    }

    private fun fallbackSort() {
        TODO()
    }

    public companion object {
        public const val DEFAULT_WORK_FACTOR: Int = 30

        private const val N_RADIX = 2
        private const val N_QSORT = 12
        private const val N_SHELL = 18
        private const val N_OVERSHOOT = N_RADIX + N_QSORT + N_SHELL + 2
    }
}
