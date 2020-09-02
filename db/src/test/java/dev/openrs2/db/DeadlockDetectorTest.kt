package dev.openrs2.db

import java.sql.SQLException
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

object DeadlockDetectorTest {
    @Test
    fun testDefault() {
        assertTrue(DefaultDeadlockDetector.isDeadlock(SQLException()))
    }

    @Test
    fun testH2() {
        assertFalse(H2DeadlockDetector.isDeadlock(SQLException()))
        assertTrue(H2DeadlockDetector.isDeadlock(SQLException(null, null, 40001)))
        assertTrue(H2DeadlockDetector.isDeadlock(SQLException(null, null, 50200)))
    }

    @Test
    fun testMysql() {
        assertFalse(MysqlDeadlockDetector.isDeadlock(SQLException()))
        assertTrue(MysqlDeadlockDetector.isDeadlock(SQLException(null, null, 1205)))
        assertTrue(MysqlDeadlockDetector.isDeadlock(SQLException(null, null, 1213)))
    }

    @Test
    fun testPostgres() {
        assertFalse(PostgresDeadlockDetector.isDeadlock(SQLException()))
        assertTrue(PostgresDeadlockDetector.isDeadlock(SQLException(null, "40001")))
        assertTrue(PostgresDeadlockDetector.isDeadlock(SQLException(null, "40P01")))
    }

    @Test
    fun testSqlite() {
        assertFalse(SqliteDeadlockDetector.isDeadlock(SQLException()))
        assertTrue(SqliteDeadlockDetector.isDeadlock(SQLException(null, null, 5)))
        assertTrue(SqliteDeadlockDetector.isDeadlock(SQLException(null, null, 6)))
    }
}
