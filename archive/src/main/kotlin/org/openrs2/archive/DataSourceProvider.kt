package org.openrs2.archive

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import javax.inject.Provider
import javax.sql.DataSource

public class DataSourceProvider : Provider<DataSource> {
    override fun get(): DataSource {
        val dataSource = PGSimpleDataSource()
        // TODO(gpe): make the URL configurable
        dataSource.setUrl("jdbc:postgresql://localhost/runearchive?user=gpe&password=gpe")

        Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:/org/openrs2/archive/migrations")
            .load()
            .migrate()

        val config = HikariConfig()
        config.dataSource = dataSource
        return HikariDataSource(config)
    }
}
