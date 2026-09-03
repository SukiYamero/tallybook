package com.kurobello.tallybook.core.database

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

internal class AndroidDatabaseDriverFactory(private val context: Context) : DatabaseDriverFactory {
  override fun createDriver(): SqlDriver =
      AndroidSqliteDriver(TallybookDatabase.Schema, context, DATABASE_NAME)
}
