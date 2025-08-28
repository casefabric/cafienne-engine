package org.cafienne.persistence.flyway

import org.flywaydb.core.api.resolver.ResolvedMigration
import slick.migration.api.flyway.VersionedMigration

trait DBSchema {

  def scripts(tablePrefix: String): Seq[ResolvedMigration]

  def migrationScripts(tablePrefix: String): Seq[VersionedMigration[_]] = scripts(tablePrefix).asInstanceOf[Seq[VersionedMigration[_]]]
}
