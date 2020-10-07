package org.openrs2.db

import java.sql.SQLException

/**
 * A vendor-neutral [DeadlockDetector], which considers every [SQLException] to
 * be a deadlock. One of the vendor-specific implementations should be used if
 * possible, which will prevent non-deadlock errors (such as unique constraint
 * violations) from being needlessly retried.
 */
public object DefaultDeadlockDetector : DeadlockDetector {
    override fun isDeadlock(ex: SQLException): Boolean {
        return true
    }
}
