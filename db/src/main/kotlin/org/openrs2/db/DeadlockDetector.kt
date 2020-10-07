package org.openrs2.db

import java.sql.SQLException

/**
 * A functional interface for checking if an [SQLException] represents a
 * deadlock. There is no standard mechanism for representing deadlock errors,
 * so vendor-specific implementations are required.
 *
 * The [Database] class already examines the entire [Throwable.cause] and
 * [SQLException.next] chain, so implementations only need to examine the
 * individual [SQLException] passed to them.
 */
public fun interface DeadlockDetector {
    /**
     * Determines whether the [SQLException] was caused by a deadlock or not.
     * @param ex the [SQLException].
     * @return `true` if so, `false` otherwise.
     */
    public fun isDeadlock(ex: SQLException): Boolean
}
