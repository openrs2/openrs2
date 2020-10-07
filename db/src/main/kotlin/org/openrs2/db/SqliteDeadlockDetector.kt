package org.openrs2.db

import java.sql.SQLException

/**
 * A [DeadlockDetector] implementation for SQLite.
 */
public object SqliteDeadlockDetector : DeadlockDetector {
    private const val BUSY = 5
    private const val LOCKED = 6

    override fun isDeadlock(ex: SQLException): Boolean {
        /*
         * https://www.sqlite.org/rescode.html documents the meaning of the
         * error codes.
         *
         * SQLITE_BUSY (5) is similar to a lock wait timeout, so it is
         * desirable to retry if we encounter it. Furthermore, in WAL mode, it
         * can be thrown immediately if a reader and writer deadlock.
         *
         * SQLITE_LOCKED (6) is normally only caused by conflicts within the
         * same connection, which will presumably happen every time we
         * re-attempt the transaction. However, there is an edge case which
         * makes retrying desirable: the error can be caused by a conflict with
         * another connection if a shared cache is used.
         *
         * SQLITE_PROTOCOL (15) has its own built-in retry/backoff logic, so I
         * have omitted it from this check.
         */
        return ex.errorCode == BUSY || ex.errorCode == LOCKED
    }
}
