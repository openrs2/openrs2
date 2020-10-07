package org.openrs2.db

import java.sql.SQLException

/**
 * A [DeadlockDetector] implementation for MySQL and MariaDB.
 */
public object MysqlDeadlockDetector : DeadlockDetector {
    private const val LOCK_WAIT_TIMEOUT = 1205
    private const val LOCK_DEADLOCK = 1213

    override fun isDeadlock(ex: SQLException): Boolean {
        // see https://dev.mysql.com/doc/mysql-errors/8.0/en/server-error-reference.html
        return ex.errorCode == LOCK_WAIT_TIMEOUT || ex.errorCode == LOCK_DEADLOCK
    }
}
