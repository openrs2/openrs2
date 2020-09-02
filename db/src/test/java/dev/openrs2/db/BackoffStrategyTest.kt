package dev.openrs2.db

import org.junit.jupiter.api.assertThrows
import kotlin.test.Test
import kotlin.test.assertEquals

object BackoffStrategyTest {
    @Test
    fun testFixedBackoff() {
        val strategy = FixedBackoffStrategy(1000)

        assertEquals(1000, strategy.getDelay(0))
        assertEquals(1000, strategy.getDelay(1))
        assertEquals(1000, strategy.getDelay(2))

        assertThrows<IllegalArgumentException> {
            strategy.getDelay(-1)
        }
    }

    @Test
    fun testBinaryExponentialBackoff() {
        assertThrows<IllegalArgumentException> {
            BinaryExponentialBackoffStrategy(0, 1)
        }

        assertThrows<IllegalArgumentException> {
            BinaryExponentialBackoffStrategy(1, 0)
        }

        assertThrows<IllegalArgumentException> {
            BinaryExponentialBackoffStrategy(1, 1).getDelay(-1)
        }
    }
}
