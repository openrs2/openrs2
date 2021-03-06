package org.openrs2.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals

class IsaacRandomTest {
    @Test
    fun testZeroSeed() {
        val random = IsaacRandom(IntArray(256))

        for (i in 0 until 256) {
            random.nextInt()
        }

        for (i in 0 until 512 step 256) {
            for (j in 0 until 256) {
                assertEquals(ZERO_SEED_VECTOR[i + 255 - j], random.nextInt())
            }
        }
    }

    @Test
    fun testNoSeed() {
        val random = IsaacRandom()

        for (i in 0 until 256) {
            random.nextInt()
        }

        for (i in 0 until 512 step 256) {
            for (j in 0 until 256) {
                assertEquals(NO_SEED_VECTOR[i + 255 - j], random.nextInt())
            }
        }
    }

    @Test
    fun testSeed() {
        val buffer = ByteBuffer.wrap("This is <i>not</i> the right mytext.".toByteArray(Charsets.US_ASCII))
            .order(ByteOrder.LITTLE_ENDIAN)
            .asIntBuffer()

        val random = IsaacRandom(IntArray(buffer.limit()) { buffer[it] })

        for (expected in SEED_VECTOR) {
            assertEquals(expected, random.nextInt())
        }
    }

    private companion object {
        // test vector from https://burtleburtle.net/bob/rand/randvect.txt
        private val ZERO_SEED_VECTOR = readVector("randvect.txt")

        // generated by changing randinit(1) to randinit(0) in readable.c
        private val NO_SEED_VECTOR = readVector("noseedvect.txt")

        // generated with https://burtleburtle.net/bob/c/randtest.c
        private val SEED_VECTOR = readVector("seedvect.txt")

        private fun readVector(file: String): IntArray {
            val input = IsaacRandomTest::class.java.getResourceAsStream("isaac/$file")
            return input.bufferedReader().useLines { lines ->
                val elements = mutableListOf<Int>()

                for (line in lines) {
                    for (i in line.indices step 8) {
                        elements += Integer.parseUnsignedInt(line, i, i + 8, 16)
                    }
                }

                elements.toIntArray()
            }
        }
    }
}
