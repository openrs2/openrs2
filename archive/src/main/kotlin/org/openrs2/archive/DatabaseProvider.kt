package org.openrs2.archive

import org.flywaydb.core.Flyway
import org.openrs2.db.Database
import org.openrs2.db.PostgresDeadlockDetector
import org.postgresql.ds.PGSimpleDataSource
import javax.inject.Provider

public class DatabaseProvider : Provider<Database> {
    override fun get(): Database {
        val dataSource = PGSimpleDataSource()
        // TODO(gpe): make the URL configurable
        dataSource.setUrl("jdbc:postgresql://localhost/runearchive?user=gpe&password=gpe")

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:/org/openrs2/archive")
            .load()
            .migrate()

        // TODO(gpe): wrap dataSource with HikariCP? how do we close the pool?

        return Database(dataSource, deadlockDetector = PostgresDeadlockDetector)
    }
}
