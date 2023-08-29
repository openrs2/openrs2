package org.openrs2.archive

import jakarta.inject.Inject
import jakarta.inject.Provider
import org.openrs2.db.Database
import org.openrs2.db.PostgresDeadlockDetector
import javax.sql.DataSource

public class DatabaseProvider @Inject constructor(
    private val dataSource: DataSource
) : Provider<Database> {
    override fun get(): Database {
        return Database(dataSource, deadlockDetector = PostgresDeadlockDetector)
    }
}
