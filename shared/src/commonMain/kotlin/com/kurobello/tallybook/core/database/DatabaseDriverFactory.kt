package com.kurobello.tallybook.core.database

import app.cash.sqldelight.db.SqlDriver

internal const val DATABASE_NAME = "tallybook.db"

// A plain interface, not an expect class: the Android binding needs a Context that iOS has no
// analogue for, tests substitute an in-memory driver, and expect/actual classes are still Beta in
// Kotlin 2.4 (they warn unless -Xexpect-actual-classes is on).
internal fun interface DatabaseDriverFactory {
  fun createDriver(): SqlDriver
}
