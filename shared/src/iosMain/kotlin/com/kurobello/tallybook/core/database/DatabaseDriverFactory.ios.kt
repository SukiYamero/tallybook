package com.kurobello.tallybook.core.database

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

internal class IosDatabaseDriverFactory : DatabaseDriverFactory {
  override fun createDriver(): SqlDriver =
      NativeSqliteDriver(TallybookDatabase.Schema, DATABASE_NAME)
}
