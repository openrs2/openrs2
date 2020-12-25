package org.openrs2.db

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

object BackoffStrategyTest {
    @Test
    fun testFixedBackoff() {
        val strategy = FixedBackoffStrategy(1000)

        assertEquals(1000, strategy.getDelay(0))
        assertEquals(1000, strategy.getDelay(1))
        assertEquals(1000, strategy.getDelay(2))

        assertFailsWith<IllegalArgumentException> {
            strategy.getDelay(-1)
        }
    }

    @Test
    fun testBinaryExponentialBackoff() {
        assertFailsWith<IllegalArgumentException> {
            BinaryExponentialBackoffStrategy(0, 1)
        }

        assertFailsWith<IllegalArgumentException> {
            BinaryExponentialBackoffStrategy(1, 0)
        }

        assertFailsWith<IllegalArgumentException> {
            BinaryExponentialBackoffStrategy(1, 1).getDelay(-1)
        }
    }
}
