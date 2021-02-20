package org.openrs2.archive

import org.openrs2.db.Database
import org.openrs2.db.PostgresDeadlockDetector
import javax.inject.Inject
import javax.inject.Provider
import javax.sql.DataSource

public class DatabaseProvider @Inject constructor(
    private val dataSource: DataSource
) : Provider<Database> {
    override fun get(): Database {
        return Database(dataSource, deadlockDetector = PostgresDeadlockDetector)
    }
}
