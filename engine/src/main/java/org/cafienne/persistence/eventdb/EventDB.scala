package org.cafienne.persistence.eventdb

import com.typesafe.scalalogging.LazyLogging
import org.cafienne.infrastructure.config.persistence.PersistenceConfig
import org.cafienne.infrastructure.config.persistence.eventdb.Profile
import org.cafienne.persistence.eventdb.schema.EventDBSchema
import org.cafienne.persistence.eventdb.schema.h2.H2EventDBSchema
import org.cafienne.persistence.eventdb.schema.postgres.PostgresEventDBSchema
import org.cafienne.persistence.eventdb.schema.sqlserver.SQLServerEventDBSchema
import slick.migration.api.flyway.SlickFlyway


class EventDB(config: PersistenceConfig) extends LazyLogging {
  if (config.initializeDatabaseSchemas && config.eventDB.isJDBC) {
    val jdbcConfig = config.eventDB.jdbcConfig
    val tablePrefix = config.tablePrefix
    val flywayTableName = config.eventDB.schemaHistoryTable

    val schema: EventDBSchema = jdbcConfig.profile match {
      case Profile.Postgres => PostgresEventDBSchema
      case Profile.SQLServer => SQLServerEventDBSchema
      case Profile.H2 =>  H2EventDBSchema
      case Profile.Unsupported => throw new IllegalArgumentException("This type of profile is not supported")
    }

    logger.info("Running event database migrations")
    lazy val db = config.eventDB.databaseConfig.db
    val flywayConfiguration = SlickFlyway(db)(schema.migrationScripts(tablePrefix))
        .baselineOnMigrate(true)
        .baselineDescription("CaseFabric EventDB")
        .baselineVersion("0.0.0")
        .table(flywayTableName)
        .outOfOrder(true)

    // Create a connection and run migration
    val flyway = flywayConfiguration.load()
    flyway.migrate()
  }
}
