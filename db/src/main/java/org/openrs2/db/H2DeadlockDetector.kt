package org.openrs2.db

import java.sql.SQLException

/**
 * A [DeadlockDetector] implementation for H2.
 */
public object H2DeadlockDetector : DeadlockDetector {
    private const val DEADLOCK_1 = 40001
    private const val LOCK_TIMEOUT_1 = 50200

    override fun isDeadlock(ex: SQLException): Boolean {
        // see https://www.h2database.com/javadoc/org/h2/api/ErrorCode.html
        return ex.errorCode == DEADLOCK_1 || ex.errorCode == LOCK_TIMEOUT_1
    }
}
