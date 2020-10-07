package org.openrs2.db

/**
 * A functional interface for calculating the delay before a transaction is
 * retried due to deadlock.
 */
public fun interface BackoffStrategy {
    /**
     * Returns the delay in milliseconds to wait for after the given
     * transaction [attempt] number.
     * @param attempt the attempt number, starting at 0 to compute the delay
     * after the first failed attempt.
     * @return the delay in milliseconds.
     */
    public fun getDelay(attempt: Int): Long
}
