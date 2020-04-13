package dev.openrs2.util.io

import org.junit.jupiter.api.Assertions.assertArrayEquals
import java.io.ByteArrayOutputStream
import kotlin.test.Test

object SkipOutputStreamTest {
    @Test
    fun testSkipBytes() {
        for (n in 0..5) {
            val expected = byteArrayOf(1, 2, 3, 4, 5).drop(n).toByteArray()

            ByteArrayOutputStream().use { out ->
                SkipOutputStream(out, n.toLong()).use { skip ->
                    skip.write(1)
                    skip.write(2)
                    skip.write(3)
                    skip.write(4)
                    skip.write(5)
                }

                assertArrayEquals(expected, out.toByteArray())
            }

            ByteArrayOutputStream().use { out ->
                SkipOutputStream(out, n.toLong()).use { skip ->
                    skip.write(byteArrayOf(1, 2, 3, 4, 5))
                }

                assertArrayEquals(expected, out.toByteArray())
            }

            ByteArrayOutputStream().use { out ->
                SkipOutputStream(out, n.toLong()).use { skip ->
                    skip.write(byteArrayOf(1, 2, 3))
                    skip.write(byteArrayOf(4, 5))
                }

                assertArrayEquals(expected, out.toByteArray())
            }

            ByteArrayOutputStream().use { out ->
                SkipOutputStream(out, n.toLong()).use { skip ->
                    skip.write(byteArrayOf(0, 1, 2, 3, 4, 5, 6), 1, 5)
                }

                assertArrayEquals(expected, out.toByteArray())
            }
        }
    }
}
