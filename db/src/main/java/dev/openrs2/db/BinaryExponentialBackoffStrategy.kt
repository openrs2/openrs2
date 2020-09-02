package dev.openrs2.db

import com.google.common.math.IntMath
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.min

/**
 * A [BackoffStrategy] that implements binary exponential backoff. It returns a
 * delay between `0` and `2**c - 1` inclusive, where `c` is the attempt
 * number (starting at 1 for the delay after the first attempt). `c` is clamped
 * at [cMax]. The delay is scaled by [scale] milliseconds.
 */
public class BinaryExponentialBackoffStrategy(
    /**
     * The maximum value of `c` (inclusive). Must be greater than zero.
     */
    private val cMax: Int,

    /**
     * The scale, in milliseconds. Must be greater than zero (use
     * [FixedBackoffStrategy] for a delay that is always zero).
     */
    private val scale: Long
) : BackoffStrategy {
    init {
        require(cMax >= 1 && scale >= 1)
    }

    override fun getDelay(attempt: Int): Long {
        require(attempt >= 0)

        val bound = IntMath.pow(2, min(attempt + 1, cMax))
        return ThreadLocalRandom.current().nextInt(bound).toLong() * scale
    }
}
