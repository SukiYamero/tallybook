package com.kurobello.tallybook.core.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver

/**
 * The stored `updated_at` and `deleted_at` of [id] in [table], read from the table itself because
 * every repository query hides tombstones.
 */
internal fun SqlDriver.storedStamps(table: String, id: String): Pair<String, String?>? =
    executeQuery(
            null,
            "SELECT updated_at, deleted_at FROM $table WHERE id = ?",
            { cursor ->
              QueryResult.Value(
                  if (cursor.next().value)
                      requireNotNull(cursor.getString(0)) to cursor.getString(1)
                  else null
              )
            },
            1,
        ) {
          bindString(0, id)
        }
        .value

/** Tombstones [id] in [table] with its current stamp, leaving any child rows in place. */
internal fun SqlDriver.tombstone(table: String, id: String) {
  execute(null, "UPDATE $table SET deleted_at = updated_at WHERE id = ?", 1) { bindString(0, id) }
}
