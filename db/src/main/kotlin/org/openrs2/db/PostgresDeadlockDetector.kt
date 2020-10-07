package org.openrs2.db

import java.sql.SQLException

/**
 * A [DeadlockDetector] implementation for PostgreSQL.
 */
public object PostgresDeadlockDetector : DeadlockDetector {
    private const val SERIALIZATION_FAILURE = "40001"
    private const val DEADLOCK_DETECTED = "40P01"

    override fun isDeadlock(ex: SQLException): Boolean {
        // see https://www.postgresql.org/docs/current/errcodes-appendix.html
        val sqlState = ex.sqlState ?: return false
        return sqlState == SERIALIZATION_FAILURE || sqlState == DEADLOCK_DETECTED
    }
}
