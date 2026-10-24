package com.kurobello.tallybook.core.data

import okio.IOException

/** Keeps a [PreferencesFile] out of platform backups and device-to-device restores. */
fun interface BackupExclusion {
  /** @throws IOException when the platform refuses to mark the file. */
  fun exclude(file: PreferencesFile)
}
