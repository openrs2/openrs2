package org.openrs2.db

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.h2.jdbcx.JdbcDataSource
import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class DatabaseTest {
    private class TestException : Exception {
        constructor() : super()
        constructor(cause: Throwable) : super(cause)
    }

    private class DeadlockException : SQLException(null, null, 40001)
    private class NonDeadlockException : SQLException()

    @Test
    fun testBounds() {
        assertFailsWith<IllegalArgumentException> {
            Database(dataSource, attempts = 0)
        }
    }

    @Test
    fun testSuccessful() = runBlockingTest {
        val start = currentTime

        val result = database.execute { connection ->
            connection.prepareStatement("VALUES 12345").use { stmt ->
                val rows = stmt.executeQuery()
                assertTrue(rows.next())
                return@execute rows.getInt(1)
            }
        }

        assertEquals(12345, result)

        val elapsed = currentTime - start
        assertEquals(0, elapsed)
    }

    @Test
    fun testSuccessfulOnce() {
        val result = database.executeOnce { connection ->
            connection.prepareStatement("VALUES 12345").use { stmt ->
                val rows = stmt.executeQuery()
                assertTrue(rows.next())
                return@executeOnce rows.getInt(1)
            }
        }

        assertEquals(12345, result)
    }

    @Test
    fun testDeadlockRetry() = runBlockingTest {
        var attempts = 0
        val start = currentTime

        val result = database.execute { connection ->
            if (attempts++ == 0) {
                throw DeadlockException()
            }

            connection.prepareStatement("VALUES 12345").use { stmt ->
                val rows = stmt.executeQuery()
                assertTrue(rows.next())
                return@execute rows.getInt(1)
            }
        }

        assertEquals(12345, result)
        assertEquals(2, attempts)

        val elapsed = currentTime - start
        assertEquals(10, elapsed)
    }

    @Test
    fun testDeadlockOnce() {
        var attempts = 0

        assertFailsWith<DeadlockException> {
            database.executeOnce<Unit> {
                attempts++
                throw DeadlockException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun testDeadlockFailure() {
        var attempts = 0

        assertFailsWith<DeadlockException> {
            runBlockingTest {
                database.execute<Unit> {
                    attempts++
                    throw DeadlockException()
                }
            }
        }

        assertEquals(ATTEMPTS, attempts)
    }

    @Test
    fun testNonDeadlockFailure() {
        var attempts = 0

        assertFailsWith<TestException> {
            runBlockingTest {
                database.execute<Unit> {
                    attempts++
                    throw TestException()
                }
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun testNonDeadlockFailureOnce() {
        var attempts = 0

        assertFailsWith<TestException> {
            database.executeOnce<Unit> {
                attempts++
                throw TestException()
            }
        }

        assertEquals(1, attempts)
    }

    @Test
    fun testDeadlockCauseChain() = runBlockingTest {
        var attempts = 0
        val start = currentTime

        val result = database.execute { connection ->
            if (attempts++ == 0) {
                throw TestException(DeadlockException())
            }

            connection.prepareStatement("VALUES 12345").use { stmt ->
                val rows = stmt.executeQuery()
                assertTrue(rows.next())
                return@execute rows.getInt(1)
            }
        }

        assertEquals(12345, result)
        assertEquals(2, attempts)

        val elapsed = currentTime - start
        assertEquals(10, elapsed)
    }

    @Test
    fun testDeadlockNextChain() = runBlockingTest {
        var attempts = 0
        val start = currentTime

        val result = database.execute { connection ->
            if (attempts++ == 0) {
                val ex = SQLException()
                ex.nextException = DeadlockException()
                throw ex
            }

            connection.prepareStatement("VALUES 12345").use { stmt ->
                val rows = stmt.executeQuery()
                assertTrue(rows.next())
                return@execute rows.getInt(1)
            }
        }

        assertEquals(12345, result)
        assertEquals(2, attempts)

        val elapsed = currentTime - start
        assertEquals(10, elapsed)
    }

    @Test
    fun testNonDeadlockNextChain() {
        var attempts = 0

        assertFailsWith<NonDeadlockException> {
            runBlockingTest {
                database.execute<Unit> {
                    attempts++

                    val ex = NonDeadlockException()
                    ex.nextException = SQLException()
                    throw ex
                }
            }
        }

        assertEquals(1, attempts)
    }

    private companion object {
        private const val DELAY = 10L
        private const val ATTEMPTS = 5

        private val dataSource = JdbcDataSource().apply {
            setUrl("jdbc:h2:mem:")
        }

        private val database = Database(
            dataSource,
            deadlockDetector = H2DeadlockDetector,
            backoffStrategy = FixedBackoffStrategy(DELAY),
            attempts = ATTEMPTS
        )
    }
}
