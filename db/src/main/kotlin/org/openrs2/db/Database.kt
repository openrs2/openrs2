package org.openrs2.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

/**
 * A thin layer on top of the JDBC API that enforces the use of transactions,
 * automatically retrying them on deadlock, and provides coroutine integration.
 *
 * Connection pooling is not provided by this library. A separate connection
 * pooling library (such as HikariCP or the functionality built into your
 * database driver) should be used in combination with this library.
 */
public class Database(
    /**
     * The [DataSource] used to obtain [Connection] objects. A new connection
     * object is opened for each transaction attempt, so a pooled [DataSource]
     * should be used.
     */
    private val dataSource: DataSource,

    /**
     * The [DeadlockDetector] used to determine if a transaction should be
     * retried. Defaults to [DefaultDeadlockDetector], which is vendor-neutral
     * but not very efficient (any error causes a transaction to be retried).
     * One of the vendor-specific implementations should be used instead for
     * optimal performance.
     */
    private val deadlockDetector: DeadlockDetector = DefaultDeadlockDetector,

    /**
     * The [BackoffStrategy] used to determine how long to wait between
     * transaction attempts. Defaults to a [BinaryExponentialBackoffStrategy]
     * with cMax=8 and scale=10 milliseconds.
     */
    private val backoffStrategy: BackoffStrategy = DEFAULT_BACKOFF_STRATEGY,

    /**
     * The nubmer of times to try executing a transaction before giving up.
     * Defaults to 5. Must be greater than zero.
     */
    private val attempts: Int = DEFAULT_ATTEMPTS
) {
    init {
        require(attempts >= 1)
    }

    /**
     * Executes a [Transaction]. If the transaction fails due to deadlock, it
     * is retried up to [attempts] times in total (including the first
     * attempt).
     *
     * The coroutine is suspended for a delay between each attempt.
     *
     * The JDBC calls will block the thread the coroutine is scheduled on. This
     * function should therefore be called within a context that uses the
     * [Dispatchers.IO] dispatcher.
     * @param transaction the transaction.
     * @return the result returned by [Transaction.execute].
     */
    public suspend fun <T> execute(transaction: Transaction<T>): T {
        for (attempt in 0 until attempts) {
            try {
                return executeOnce(transaction)
            } catch (t: Throwable) {
                if (isDeadlock(t) && attempt != attempts - 1) {
                    val backoff = backoffStrategy.getDelay(attempt)
                    delay(backoff)
                    continue
                }

                throw t
            }
        }

        throw AssertionError()
    }

    private fun <T> executeOnce(transaction: Transaction<T>): T {
        dataSource.connection.use { connection ->
            val oldAutoCommit = connection.autoCommit
            connection.autoCommit = false

            try {
                val result = try {
                    transaction.execute(connection)
                } catch (t: Throwable) {
                    connection.rollback()
                    throw t
                }

                connection.commit()

                return result
            } finally {
                connection.autoCommit = oldAutoCommit
            }
        }
    }

    private fun isDeadlock(t: Throwable): Boolean {
        if (t is SQLException) {
            return isDeadlock(t)
        }

        val cause = t.cause
        if (cause != null) {
            return isDeadlock(cause)
        }

        return false
    }

    private fun isDeadlock(ex: SQLException): Boolean {
        if (deadlockDetector.isDeadlock(ex)) {
            return true
        }

        val next = ex.nextException
        if (next != null) {
            return isDeadlock(next)
        }

        return false
    }

    private companion object {
        private const val DEFAULT_ATTEMPTS = 5
        private val DEFAULT_BACKOFF_STRATEGY = BinaryExponentialBackoffStrategy(8, 10)
    }
}
