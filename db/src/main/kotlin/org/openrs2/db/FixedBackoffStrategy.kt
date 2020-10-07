package org.openrs2.db

/**
 * A [BackoffStrategy] with a fixed delay.
 *
 * It permits a delay of zero, which is appropriate for use with database
 * servers that allow one of the deadlocked connections to proceed (thus
 * guaranteeing forward progress) and where you only expect a small amount of
 * lock contention.
 */
public class FixedBackoffStrategy(
    /**
     * The delay in milliseconds. Must be zero or positive.
     */
    private val delay: Long
) : BackoffStrategy {
    init {
        delay >= 0
    }

    override fun getDelay(attempt: Int): Long {
        require(attempt >= 0)
        return delay
    }
}
